#!/usr/bin/env python3
"""Drive meter to cash through rating, true-up, disconnect, and overlap."""

import base64
import json
import sys
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor

PORTS = {
    "account": 8241,
    "premise": 8242,
    "meter": 8243,
    "reading": 8244,
    "tariff": 8245,
    "charge": 8246,
    "bill": 8247,
    "adjustment": 8248,
    "notice": 8249,
    "balance": 8250,
    "payment": 8251,
    "field": 8252,
    "audit": 8253,
}
MGMT = "http://127.0.0.1:15685"
AUTH = "platform:platform"
TENDER_DLQ = "meter.payment.tender.dlq"
FIELD_DLQ = "meter.field.cut.dlq"
failures = []
RUN = uuid.uuid4().hex[:8]


def check(name, ok, detail=""):
    if ok:
        print(f"PASS  {name}", flush=True)
    else:
        print(f"FAIL  {name} {detail}", flush=True)
        failures.append(f"{name}: {detail}")


def call(method, url, body=None, auth=None, timeout=20):
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        request.add_header("content-type", "application/json")
    if auth:
        request.add_header("Authorization", "Basic " + base64.b64encode(auth.encode()).decode())
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode()
            return response.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as ex:
        raw = ex.read().decode()
        try:
            parsed = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            parsed = raw
        return ex.code, parsed
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError):
        return 0, None


def wait_ok(url, seconds=300, auth=None):
    deadline = time.time() + seconds
    while time.time() < deadline:
        if call("GET", url, auth=auth)[0] == 200:
            return True
        time.sleep(1)
    return False


def until(label, fn, seconds=120):
    deadline = time.time() + seconds
    last = None
    while time.time() < deadline:
        last = fn()
        if last is not None:
            return last
        time.sleep(0.4)
    check(label, False, f"timed out last={last!r}")
    return None


def depth(name):
    status, body = call("GET", f"{MGMT}/api/queues/%2F/{name}", auth=AUTH)
    if status != 200 or not isinstance(body, dict):
        return None
    return body.get("messages", 0)


def grew(name, before, amount=1):
    def ready():
        now = depth(name)
        if now is not None and now >= before + amount:
            return now
        return None
    return until(f"dlq {name}", ready, 120)


def base(name):
    return f"http://127.0.0.1:{PORTS[name]}"


def ready_dir(service, path):
    def seen():
        code, body = call("GET", f"{base(service)}{path}")
        if code == 200 and isinstance(body, dict):
            return body
        return None
    return until(f"{service} {path}", seen, 120) is not None


def open_account(number, name):
    status, body = call("POST", f"{base('account')}/accounts", {"accountNumber": number, "name": name})
    check(f"account {number}", status == 201 and isinstance(body, dict), body)
    if status != 201 or not isinstance(body, dict):
        return None
    account_id = body["id"]
    check(f"premise sees {number}", ready_dir("premise", f"/directory/accounts/{account_id}"))
    check(f"bill sees {number}", ready_dir("bill", f"/directory/accounts/{account_id}"))
    return account_id


def open_premise(account_id, code):
    status, body = call("POST", f"{base('premise')}/premises", {
        "premiseCode": code, "accountId": account_id, "address": f"{code} Elm",
    })
    check(f"premise {code}", status == 201 and isinstance(body, dict), body)
    if status != 201 or not isinstance(body, dict):
        return None
    premise_id = body["id"]
    check(f"meter sees {code}", ready_dir("meter", f"/directory/premises/{premise_id}"))
    check(f"bill sees {code}", ready_dir("bill", f"/directory/premises/{premise_id}"))
    return premise_id


def open_meter(premise_id, serial, last_read):
    status, body = call("POST", f"{base('meter')}/meters", {
        "serial": serial, "premiseId": premise_id, "lastRead": last_read,
    })
    check(f"meter {serial}", status == 201 and isinstance(body, dict), body)
    if status != 201 or not isinstance(body, dict):
        return None
    meter_id = body["id"]
    check(f"bill sees {serial}", ready_dir("bill", f"/directory/meters/{meter_id}"))
    return meter_id


def open_tariff(code, rate):
    status, body = call("POST", f"{base('tariff')}/tariffs", {"code": code, "rateCents": rate})
    check(f"tariff {code}", status == 201 and isinstance(body, dict), body)
    if status != 201 or not isinstance(body, dict):
        return None
    tariff_id = body["id"]
    check(f"bill sees {code}", ready_dir("bill", f"/directory/tariffs/{tariff_id}"))
    return tariff_id


def world(prefix, last_read, rate):
    label = f"{prefix}-{RUN}"
    account_id = open_account(f"{label}-A", prefix)
    if not account_id:
        return None
    premise_id = open_premise(account_id, f"{label}-P")
    if not premise_id:
        return None
    meter_id = open_meter(premise_id, f"{label}-M", last_read)
    if not meter_id:
        return None
    tariff_id = open_tariff(f"{label}-T", rate)
    if not tariff_id:
        return None
    return {
        "account": account_id,
        "premise": premise_id,
        "meter": meter_id,
        "tariff": tariff_id,
        "last": last_read,
    }


def start_bill(row, read_value, route, request_id=None):
    request_id = request_id or str(uuid.uuid4())
    status, body = call("POST", f"{base('bill')}/bills", {
        "requestId": request_id,
        "accountId": row["account"],
        "premiseId": row["premise"],
        "meterId": row["meter"],
        "tariffId": row["tariff"],
        "readValue": read_value,
        "route": route,
    })
    return status, body, request_id


def load_bill(bill_id):
    code, body = call("GET", f"{base('bill')}/bills/{bill_id}")
    if code == 200 and isinstance(body, dict):
        return body
    return None


def wait_status(bill_id, status, seconds=120):
    def ready():
        body = load_bill(bill_id)
        if body and body.get("status") == status:
            return body
        return None
    return until(f"{bill_id} {status}", ready, seconds)


def wait_terminal(bill_id, allowed, seconds=120):
    def ready():
        body = load_bill(bill_id)
        if body and body.get("status") in allowed:
            return body
        return None
    return until(f"{bill_id} {allowed}", ready, seconds)


def meter_of(meter_id):
    code, body = call("GET", f"{base('meter')}/meters/{meter_id}")
    if code == 200 and isinstance(body, dict):
        return body
    return None


def premise_of(premise_id):
    code, body = call("GET", f"{base('premise')}/premises/{premise_id}")
    if code == 200 and isinstance(body, dict):
        return body
    return None


def balance_of(bill_id):
    code, body = call("GET", f"{base('balance')}/balances/{bill_id}")
    return code, body


def pay(bill_id, token, request_id=None):
    return call("POST", f"{base('bill')}/bills/{bill_id}/payments", {
        "requestId": request_id or str(uuid.uuid4()),
        "payToken": token,
    })


def disconnect(bill_id, seal, request_id=None):
    return call("POST", f"{base('bill')}/bills/{bill_id}/disconnects", {
        "requestId": request_id or str(uuid.uuid4()),
        "sealToken": seal,
    })


def audits(bill_id):
    code, body = call("GET", f"{base('audit')}/audits/{bill_id}")
    if code == 200 and isinstance(body, list):
        return body
    return None


def fact_count(bill_id, kind, status):
    def ready():
        rows = audits(bill_id)
        if rows is None:
            return None
        count = sum(1 for row in rows if row.get("kind") == kind and row.get("status") == status)
        if count > 0:
            return count
        return None
    return until(f"audit {kind} {status}", ready, 90)


def settled_money(bill_id, premise_id):
    def ready():
        bill = load_bill(bill_id)
        premise = premise_of(premise_id)
        code, balance = balance_of(bill_id)
        if not bill or not premise or code != 200 or not isinstance(balance, dict):
            return None
        if bill.get("openCents") == 0 and balance.get("openCents") == 0 and premise.get("serviceState") == "ON" \
                and bill.get("status") in ("PAID", "SETTLED"):
            return bill
        return None
    return until(f"{bill_id} settled", ready, 120)


def hold(bill_id, premise_id, seconds=8):
    deadline = time.time() + seconds
    seen = None
    while time.time() < deadline:
        bill = load_bill(bill_id)
        premise = premise_of(premise_id)
        code, balance = balance_of(bill_id)
        ok = bill and premise and code == 200 and isinstance(balance, dict) \
            and bill.get("status") in ("PAID", "SETTLED") \
            and bill.get("openCents") == 0 \
            and balance.get("openCents") == 0 \
            and premise.get("serviceState") == "ON"
        if not ok:
            return False, bill, premise, balance
        seen = bill.get("status")
        time.sleep(1)
    return True, seen, None, None


def main():
    for name in PORTS:
        check(f"health {name}", wait_ok(f"{base(name)}/actuator/health"))
    if failures:
        return 1

    check("blank account", call("POST", f"{base('account')}/accounts", {"accountNumber": " ", "name": "x"})[0] == 400)
    check("negative rate", call("POST", f"{base('tariff')}/tariffs", {"code": "NEG", "rateCents": -1})[0] == 400)
    check("negative dial", call("POST", f"{base('meter')}/meters", {
        "serial": "NEG-M", "premiseId": "missing", "lastRead": -1,
    })[0] == 400)
    dup = f"DUP-{RUN}-A"
    first = open_account(dup, "Dup")
    if first:
        check("duplicate account", call("POST", f"{base('account')}/accounts", {
            "accountNumber": dup, "name": "Dup",
        })[0] == 409)
        missing = call("POST", f"{base('premise')}/premises", {
            "premiseCode": "GHOST", "accountId": "missing-account", "address": "nowhere",
        })
        check("unknown account", missing[0] == 404, missing)

    left = world("LEFT", 1000, 10)
    right = world("RIGHT", 1000, 10)
    if left and right:
        mismatch = start_bill({
            "account": left["account"],
            "premise": right["premise"],
            "meter": right["meter"],
            "tariff": right["tariff"],
        }, 1100, "ROUTE-1")
        check("mismatched premise", mismatch[0] == 409, mismatch[1])
        serial = call("POST", f"{base('meter')}/meters", {
            "serial": f"LEFT-{RUN}-M", "premiseId": left["premise"], "lastRead": 1,
        })
        check("duplicate serial", serial[0] == 409, serial)

    happy = world("HAPPY", 1000, 10)
    if happy:
        status, body, request_id = start_bill(happy, 1100, "ROUTE-1")
        check("happy create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            bill_id = body["id"]
            again = start_bill(happy, 1100, "ROUTE-1", request_id)
            check("replay create", again[0] == 200 and again[1].get("id") == bill_id, again)
            noticed = wait_status(bill_id, "NOTICED")
            if noticed:
                reading = call("GET", f"{base('reading')}/readings/{bill_id}")
                charge = call("GET", f"{base('charge')}/charges/{bill_id}")
                check("happy consumption", reading[0] == 200 and reading[1].get("consumption") == 100, reading[1])
                check("happy charge", charge[0] == 200 and charge[1].get("amountCents") == 1000, charge[1])
                notice = call("GET", f"{base('notice')}/notices/{bill_id}")
                check("happy notice", notice[0] == 200 and isinstance(notice[1], dict) and notice[1].get("status") == "ISSUED", notice[1])
                paid_request = str(uuid.uuid4())
                paid = pay(bill_id, "PAY0001", paid_request)
                check("happy pay", paid[0] == 202, paid)
                done = wait_status(bill_id, "PAID")
                if done:
                    code, balance = balance_of(bill_id)
                    premise = premise_of(happy["premise"])
                    dial = meter_of(happy["meter"])
                    check("happy open", code == 200 and balance.get("openCents") == 0 and balance.get("status") == "PAID", balance)
                    check("happy premise", premise and premise.get("serviceState") == "ON", premise)
                    check("happy dial", dial and dial.get("lastRead") == 1100, dial)
                    check("happy bill open", done.get("openCents") == 0, done)
                    replay = pay(bill_id, "PAY0001", paid_request)
                    check("replay pay", replay[0] == 200, replay)
                    dial_again = meter_of(happy["meter"])
                    check("dial stable", dial_again and dial_again.get("lastRead") == 1100, dial_again)
                    check("second pay", pay(bill_id, "PAY0002")[0] == 409)
                    check("actual closed", call("POST", f"{base('bill')}/bills/{bill_id}/readings", {"readValue": 1200})[0] == 409)
                    rows = audits(bill_id)
                    check("audit paid", rows is not None and any(
                        row.get("kind") == "BillFact" and row.get("status") == "PAID" for row in rows
                    ), rows)

    estimate = world("EST", 1000, 10)
    if estimate:
        status, body, _request = start_bill(estimate, 1200, "ROUTE-ESTIMATE")
        check("estimate create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            bill_id = body["id"]
            rated = wait_status(bill_id, "RATED")
            if rated:
                check("estimate amount", rated.get("amountCents") == 2000, rated)
                check("notice waits", call("GET", f"{base('notice')}/notices/{bill_id}")[0] == 404)
                check("balance waits", balance_of(bill_id)[0] == 404)
                dial = meter_of(estimate["meter"])
                check("dial waits", dial and dial.get("lastRead") == 1000, dial)
                actual = call("POST", f"{base('bill')}/bills/{bill_id}/readings", {"readValue": 1100})
                check("actual accepted", actual[0] == 202, actual)
                noticed = wait_status(bill_id, "NOTICED")
                if noticed:
                    adjustment = call("GET", f"{base('adjustment')}/adjustments/{bill_id}")
                    code, balance = balance_of(bill_id)
                    dial = meter_of(estimate["meter"])
                    reading = call("GET", f"{base('reading')}/readings/{bill_id}")
                    check("adjustment", adjustment[0] == 200 and adjustment[1].get("deltaCents") == -1000, adjustment[1])
                    check("open after true-up", noticed.get("amountCents") == 1000 and noticed.get("openCents") == 1000, noticed)
                    check("balance after true-up", code == 200 and balance.get("openCents") == 1000, balance)
                    check("dial after true-up", dial and dial.get("lastRead") == 1100, dial)
                    check("actual consumption", reading[0] == 200 and reading[1].get("consumption") == 100
                          and reading[1].get("status") == "ACTUAL", reading[1])
                    notice = call("GET", f"{base('notice')}/notices/{bill_id}")
                    check("notice after true-up", notice[0] == 200 and isinstance(notice[1], dict) and notice[1].get("status") == "ISSUED", notice[1])
                    check("estimate pay", pay(bill_id, "PAY0001")[0] == 202)
                    done = wait_status(bill_id, "PAID")
                    if done:
                        code, balance = balance_of(bill_id)
                        check("estimate collected", done.get("openCents") == 0 and code == 200 and balance.get("openCents") == 0, balance)

    bad = world("BAD", 1000, 10)
    if bad:
        status, body, _request = start_bill(bad, 999, "ROUTE-1")
        check("bad create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            bill_id = body["id"]
            cancelled = wait_status(bill_id, "CANCELLED")
            if cancelled:
                dial = meter_of(bad["meter"])
                check("bad reason", cancelled.get("reason") == "READ", cancelled)
                check("bad dial", dial and dial.get("lastRead") == 1000, dial)
                check("bad charge", call("GET", f"{base('charge')}/charges/{bill_id}")[0] == 404)
                check("bad reading", call("GET", f"{base('reading')}/readings/{bill_id}")[0] == 404)
                check("pay cancelled", pay(bill_id, "PAY0001")[0] == 409)
                check("cut cancelled", disconnect(bill_id, "METER-1")[0] == 409)

    empty = world("EMPTY", 1000, 10)
    if empty:
        status, body, _request = start_bill(empty, 1000, "ROUTE-1")
        check("empty create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            cancelled = wait_status(body["id"], "CANCELLED")
            if cancelled:
                check("empty reason", cancelled.get("reason") == "EMPTY", cancelled)
                dial = meter_of(empty["meter"])
                check("empty dial", dial and dial.get("lastRead") == 1000, dial)

    overflow = world("OVER", 5000, 2147483647)
    if overflow:
        status, body, _request = start_bill(overflow, 5002, "ROUTE-1")
        check("overflow create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            bill_id = body["id"]

            def rewound():
                bill = load_bill(bill_id)
                dial = meter_of(overflow["meter"])
                if bill and dial and bill.get("status") == "CANCELLED" and bill.get("reason") == "PRICE" \
                        and dial.get("lastRead") == 5000:
                    return bill
                return None

            cancelled = until("overflow rewound", rewound, 120)
            if cancelled:
                check("overflow charge", call("GET", f"{base('charge')}/charges/{bill_id}")[0] == 404)

    decline = world("DECLINE", 1000, 10)
    if decline:
        status, body, _request = start_bill(decline, 1100, "ROUTE-1")
        check("decline create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            bill_id = body["id"]
            noticed = wait_status(bill_id, "NOTICED")
            if noticed:
                before = depth(TENDER_DLQ)
                check("decline depth", before is not None, before)
                declined = pay(bill_id, "PAY0000")
                check("decline accepted", declined[0] == 202, declined)
                if before is not None:
                    grew(TENDER_DLQ, before)
                still = load_bill(bill_id)
                code, balance = balance_of(bill_id)
                payments = call("GET", f"{base('payment')}/payments/{bill_id}")
                check("decline stays noticed", still and still.get("status") == "NOTICED" and still.get("openCents") == 1000, still)
                check("decline balance", code == 200 and balance.get("openCents") == 1000 and balance.get("status") == "OPEN", balance)
                check("payments empty", payments[0] == 200 and payments[1] == [], payments[1])

    sealed = world("SEAL", 1000, 10)
    if sealed:
        status, body, _request = start_bill(sealed, 1100, "ROUTE-1")
        check("sealed create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            bill_id = body["id"]
            noticed = wait_status(bill_id, "NOTICED")
            if noticed:
                before = depth(FIELD_DLQ)
                check("sealed depth", before is not None, before)
                cut = disconnect(bill_id, "METER-SEALED")
                check("sealed accepted", cut[0] == 202, cut)
                if before is not None:
                    grew(FIELD_DLQ, before)
                still = load_bill(bill_id)
                premise = premise_of(sealed["premise"])
                check("sealed stays noticed", still and still.get("status") == "NOTICED", still)
                check("sealed premise", premise and premise.get("serviceState") == "ON", premise)
                check("sealed cut absent", call("GET", f"{base('field')}/cuts/{bill_id}")[0] == 404)

    cut = world("CUT", 1000, 10)
    if cut:
        status, body, _request = start_bill(cut, 1100, "ROUTE-1")
        check("cut create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            bill_id = body["id"]
            noticed = wait_status(bill_id, "NOTICED")
            if noticed:
                check("cut command", disconnect(bill_id, "METER-1")[0] == 202)

                def off():
                    bill = load_bill(bill_id)
                    premise = premise_of(cut["premise"])
                    code, balance = balance_of(bill_id)
                    if bill and premise and code == 200 and bill.get("status") in ("CUT", "CUT_OFF") \
                            and premise.get("serviceState") == "OFF" and balance.get("openCents") == 1000:
                        return bill
                    return None

                held = until("premise off", off, 120)
                if held:
                    check("restore pay", pay(bill_id, "PAY0001")[0] == 202)
                    done = wait_status(bill_id, "SETTLED")
                    if done:
                        premise = premise_of(cut["premise"])
                        code, balance = balance_of(bill_id)
                        visit = call("GET", f"{base('field')}/cuts/{bill_id}")
                        check("restored premise", premise and premise.get("serviceState") == "ON", premise)
                        check("restored open", done.get("openCents") == 0 and code == 200 and balance.get("openCents") == 0, balance)
                        check("restored visit", visit[0] == 200 and visit[1].get("status") == "RESTORED", visit[1])

    pair = world("PAIR", 1000, 10)
    if pair:
        status, body, _request = start_bill(pair, 1100, "ROUTE-1")
        check("pair create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            bill_id = body["id"]
            noticed = wait_status(bill_id, "NOTICED")
            if noticed:
                with ThreadPoolExecutor(max_workers=2) as pool:
                    first_pay = pool.submit(pay, bill_id, "PAY0001")
                    second_pay = pool.submit(pay, bill_id, "PAY0002")
                    results = [first_pay.result(), second_pay.result()]
                check("two tenders", all(code == 202 for code, _body in results), results)
                applied = fact_count(bill_id, "BalanceEvent", "APPLIED")
                short = fact_count(bill_id, "BalanceEvent", "SHORT")
                check("one applied", applied == 1, applied)
                check("one short", short == 1, short)
                done = wait_status(bill_id, "PAID")
                if done:
                    premise = premise_of(pair["premise"])
                    code, balance = balance_of(bill_id)
                    check("pair open", done.get("openCents") == 0 and code == 200 and balance.get("openCents") == 0, balance)
                    check("pair premise", premise and premise.get("serviceState") == "ON", premise)

    race = world("RACE", 1000, 10)
    if race:
        status, body, _request = start_bill(race, 1100, "ROUTE-1")
        check("race create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            bill_id = body["id"]
            noticed = wait_status(bill_id, "NOTICED")
            if noticed:
                with ThreadPoolExecutor(max_workers=2) as pool:
                    tender = pool.submit(pay, bill_id, "PAY0001")
                    cut_race = pool.submit(disconnect, bill_id, "METER-OK")
                    results = [tender.result(), cut_race.result()]
                check("race accepted", all(code == 202 for code, _body in results), results)
                done = settled_money(bill_id, race["premise"])
                if done:
                    steady, seen, premise, balance = hold(bill_id, race["premise"], 8)
                    check("race steady", steady, {"status": seen, "premise": premise, "balance": balance})
                    check("race ended", done.get("status") in ("PAID", "SETTLED"), done)

    check("unknown bill", call("GET", f"{base('bill')}/bills/{uuid.uuid4()}")[0] == 404)
    check("empty audit", call("GET", f"{base('audit')}/audits/{uuid.uuid4()}")[1] == [])

    if failures:
        print(f"{len(failures)} failed", flush=True)
        for failure in failures:
            print(f" - {failure}", flush=True)
        return 1
    print("meter to cash smoke passed", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
