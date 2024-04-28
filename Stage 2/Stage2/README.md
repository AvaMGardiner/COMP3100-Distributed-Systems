# Running the script
Ensure that ds-server, ds-client, and your client are in this directory.
Run `s2_test.py`, passing the command to run your client as a string and -n if your client uses newline-terminated messages (i.e. Java clients).
You can specify a directory of config files with -c (`S2TestConfigs` is used if you omit -c).
For example:
```
python3 ./s2_test.py "java MyClient" -n -c S2TestConfigs
```

This script may take a long time to complete as it needs to collect results from the reference client with the relevant algorithms.
These results are stored in `results/ref_results.json`.
Reference result collection can be skipped with -r, specifying the path to a reference results file that has already been generated.
For example:
```
python3 ./s2_test.py "java MyClient" -n -r results/ref_results.json
```

# Output explanation
When the script is finished, it will print out three tables (one for each metric).
The leftmost column lists the configuration files.
The middle columns list the results of each baseline algorithm for each respective config.
The rightmost column lists your client's results for each respective config.
After the config rows, there is a row of averages and several rows with normalised values.
These final values are the average result of each column normalised against the average value of that row's algorithm.
The last "average" row depicts the average result of each column against the average results of all baseline algorithms.

The colour of values in the "Yours" column indicates your clients performance in comparison to the baseline algorithms.
Green indicates your client outperformed all baseline algorithms.
Yellow indicates your client outperformed at least one baseline algorithms.
Red indicates your client performed worse than all baseline algorithms.

The final results are shown after the tables.
2.1 indicates that your client has successfully scheduled all jobs in every config.
2.2 indicates that your client has outperformed the average of at least one baseline algorithm for all metrics.
2.3 indicates the number of configs where your client outperforms all baseline algorithms for average turnaround time (maximum of 7).
