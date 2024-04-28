#!/usr/bin/env python3
import glob
import json
import os
import re
import subprocess
import sys
from pathlib import Path
from time import sleep
from typing import Union, Dict, List

from mark_client import check_required

RefResultDict = Dict[str, Dict[str, Dict[str, Union[int, float]]]]

re_time = re.compile(r".*avg turnaround time: (\d+)")
re_util = re.compile(r".*avg util: (\d+\.?\d*).*")
re_cost = re.compile(r".*total cost: \$(\d+\.?\d*)")

algos = ["ff", "bf", "ffq", "bfq", "wfq"]


def parse_ref_results(config_dir: str, metrics: List[str]) -> RefResultDict:
    results: RefResultDict = {metric: {} for metric in metrics}

    for config in sorted(glob.glob(os.path.join(config_dir, "*.xml"))):
        config_name = os.path.basename(config)
        print("Running reference client with", config_name)

        results["Turnaround time"][config_name] = {}
        results["Resource utilisation"][config_name] = {}
        results["Total rental cost"][config_name] = {}

        for algo in algos:
            server_process = subprocess.Popen(["./ds-server", "-c", config, "-v", "brief"],
                                              stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            sleep(4)
            client_process = subprocess.Popen(["./ds-client", "-a", algo],
                                              stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            server_out, server_err = server_process.communicate()
            client_out, client_err = client_process.communicate()

            server_process.wait()
            client_process.wait()

            if server_err:
                print("Error encountered by ds-server:\n", server_err.decode("utf-8"), file=sys.stderr)
            if client_err:
                print("Error encountered by ds-client:\n", client_err.decode("utf-8"), file=sys.stderr)

            lines = list(map(str, server_out.splitlines()[-3:]))
            time_match = re_time.match(lines[2])
            util_match = re_util.match(lines[1])
            cost_match = re_cost.match(lines[1])

            if not time_match or not util_match or not cost_match:
                print("Error: could not parse server output", file=sys.stderr)
                continue

            time = time_match.group(1)
            util = util_match.group(1)
            cost = cost_match.group(1)

            results["Turnaround time"][config_name][algo] = int(time)
            results["Resource utilisation"][config_name][algo] = float(util)
            results["Total rental cost"][config_name][algo] = float(cost)
    return results


if __name__ == '__main__':
    from argparse import ArgumentParser

    parser = ArgumentParser(description="Collect reference results from ds-client")
    parser.add_argument("-c", "--config_dir", default="S2DemoConfigs", help="config directory")
    parser.add_argument("-o", "--output", default="./results/ref_results.json", help="output file")
    args = parser.parse_args()

    check_required(args.config_dir)

    test_metrics = ["Turnaround time", "Resource utilisation", "Total rental cost"]
    res = parse_ref_results(args.config_dir, test_metrics)

    print(json.dumps(res, indent=2))

    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    with open(out_path, 'w') as f:
        json.dump(res, f, indent=2)
