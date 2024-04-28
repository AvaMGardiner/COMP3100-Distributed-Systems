#!/usr/bin/env python3
import glob
import json
import operator
import os
import re
import subprocess
import sys
from pathlib import Path
from statistics import mean
from time import sleep
from typing import Union, Dict, List

BOLD = "\033[1m"
RED = "\033[31m"
YELLOW = "\033[33m"
GREEN = "\033[32m"
END = "\033[0m"

ClientResultDict = Dict[str, Dict[str, Union[int, float, None]]]
RefResultDict = Dict[str, Dict[str, Dict[str, Union[int, float]]]]

re_time = re.compile(r".*avg turnaround time: (\d+)")
re_util = re.compile(r".*avg util: (\d+\.?\d*).*")
re_cost = re.compile(r".*total cost: \$(\d+\.?\d*)")

baseline = ["ff", "bf", "ffq", "bfq", "wfq"]
base_num = len(baseline)

config_width = 28
metric_width = 10
bold_width = metric_width + len(BOLD) + len(END)
colour_width = metric_width + len(RED) + len(END)
base_row_template = "|".join(["{{:<{}}}"] * (base_num + 2))
plain_row_template = base_row_template.format(*[config_width] + [metric_width] * (base_num + 1))
normal_row_template = base_row_template.format(*[config_width] + [metric_width] * base_num + [colour_width])
average_row_template = base_row_template.format(
    *[config_width + len(BOLD) + len(END)] + [bold_width] * base_num + [bold_width + len(RED)])


def colour_text(metric_: int, score_: int, template: str):
    if score_ == base_num:
        return template.format(GREEN, metric_, END)
    elif score_ > 0:
        return template.format(YELLOW, metric_, END)
    else:
        return template.format(RED, metric_, END)


def check_required(config_dir: str):
    conf_dir = Path(config_dir)
    if not conf_dir.exists():
        print("Error: config directory '{}' does not exist".format(conf_dir), file=sys.stderr)
        sys.exit(1)

    ds_server = Path("./ds-server")
    if not ds_server.exists():
        print("Error: ds-server does not exist", file=sys.stderr)
        sys.exit(1)


def parse_client_results(conf_dir: str, metrics: List[str], command: str, newline: bool) -> ClientResultDict:
    results: ClientResultDict = {metric: {} for metric in metrics}

    for config in sorted(glob.glob(os.path.join(conf_dir, "*.xml"))):
        config_name = os.path.basename(config)
        print("Running client with", config_name)

        results["Turnaround time"][config_name] = None
        results["Resource utilisation"][config_name] = None
        results["Total rental cost"][config_name] = None

        server_command = ["./ds-server", "-c", config, "-v", "brief"]
        if newline:
            server_command.append("-n")

        server_p = subprocess.Popen(server_command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        sleep(4)
        client_p = subprocess.Popen(command.split())
        server_out, server_err = server_p.communicate()

        server_p.wait()
        client_p.wait()

        if server_err:
            print("Error encountered by ds-server:\n", server_err.decode("utf-8"), file=sys.stderr)

        server_lines = server_out.splitlines()

        if len(server_lines) < 3:
            print("Error: could not parse server output", file=sys.stderr)
            continue

        lines = list(map(str, server_lines[-3:]))
        time_match = re_time.match(lines[2])
        util_match = re_util.match(lines[1])
        cost_match = re_cost.match(lines[1])

        if not time_match or not util_match or not cost_match:
            print("Error: could not parse server output", file=sys.stderr)
            continue

        time = time_match.group(1)
        util = util_match.group(1)
        cost = cost_match.group(1)

        results["Turnaround time"][config_name] = int(time)
        results["Resource utilisation"][config_name] = float(util)
        results["Total rental cost"][config_name] = float(cost)

    res_path = Path("results/test_results.json")
    res_path.parent.mkdir(parents=True, exist_ok=True)

    with open(res_path, 'w') as client_results_file:
        json.dump(results, client_results_file, indent=2)

    print()
    return results


def print_results(client_results: ClientResultDict, ref_results: RefResultDict, metrics: List[str], objective: str):
    mark_21 = 1
    baseline_scores = {}
    average_scores = {}
    algos = baseline + ["student"]

    for metric in metrics:
        if not client_results[metric] or all(value is None for value in client_results[metric].values()):
            print("Error: no results for {}".format(metric), file=sys.stderr)
            mark_21 = 0
            continue

        print(metric)
        print(plain_row_template.format(*["Config"] + list(map(str.upper, baseline)) + ["Yours"]))

        baseline_scores[metric] = {}
        comp = operator.gt if metric == "Resource utilisation" else operator.lt

        for config, res in client_results[metric].items():
            if not res:
                print("No results found for", config)
                mark_21 = 0
                continue

            baseline_score = 0
            for algo in baseline:
                if comp(res, ref_results[metric][config][algo]):
                    baseline_score += 1

            baseline_scores[metric][config] = baseline_score

            precision = "{:.2f}" if metric != "Turnaround time" else "{}"
            normal_row_vals = (
                    [config] +
                    [precision.format(ref_results[metric][config][algo]) for algo in baseline] +
                    [colour_text(res, baseline_score, "{{}}{}{{}}".format(precision))]
            )
            print(normal_row_template.format(*normal_row_vals))

        averages = (
                {algo: mean(res_dict[algo] for config, res_dict in ref_results[metric].items()) for algo in baseline} |
                {"student": mean(res for config, res in client_results[metric].items() if res)}
        )

        average_score = 0
        for algo in baseline:
            if comp(averages["student"], averages[algo]):
                average_score += 1

        average_scores[metric] = average_score

        student_average_string = BOLD + colour_text(averages["student"], average_score, "{}{:.2f}{}")
        averages_string = (
                ["{}{}{}".format(BOLD, "Average", END)] +
                ["{}{:.2f}{}".format(BOLD, averages[algo], END) for algo in baseline] +
                [student_average_string]
        )
        print(average_row_template.format(*averages_string))
        baseline_average = mean((averages[algo] for algo in baseline))

        normalised_results = {base: {algo: averages[algo] / averages[base] for algo in algos} for base in baseline}
        normalised_baseline = {algo: averages[algo] / baseline_average for algo in algos}

        for base in baseline:
            norm_string = (
                    ["Normalised ({})".format(base.upper())] +
                    ["{:.4f}".format(normalised_results[base][algo]) for algo in algos]
            )
            print(plain_row_template.format(*norm_string))

        norm_baseline_string = (
                ["Normalised (Average)"] +
                ["{:.4f}".format(normalised_baseline[algo]) for algo in algos]
        )
        print(plain_row_template.format(*norm_baseline_string))
        print()

    mark_22 = 0
    mark_23 = 0
    max_23 = 7
    objectives = ["tt", "ru", "co"]
    objective_dict = {objective: metric for objective, metric in zip(objectives, metrics)}

    if average_scores and all(score > 0 for score in average_scores.values()):
        mark_22 = 1
    if objective_dict[objective] in baseline_scores:
        mark_23 = min(
            max_23, sum(1 for score in baseline_scores[objective_dict[objective]].values() if score == base_num))

    print("Final results:")
    print("2.1: {}/1".format(mark_21))
    print("2.2: {}/1".format(mark_22))
    print("2.3: {}/{}".format(mark_23, max_23))
