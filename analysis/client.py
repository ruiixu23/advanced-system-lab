import os
import re
import numpy as np

number_re = re.compile(r'\d+')
log2_dist_line_re = re.compile(r'\s*\d+:')

OPERATION_GET = 'g'
OPERATION_SET = 's'
OPERATION_ALL = 'a'

TIMES = [2 ** i / 1000 for i in range(0, 32, 1)]


def _parse_operation_entry(entry):
    parts = entry.split()
    return {
        'time': int(parts[1]),
        'num_ops': int(parts[2]),
        'tp': int(parts[3]),
        'net': float(parts[4]),
        'get_miss': int(parts[5]),
        'rt_min': float(parts[6]) / 1000,
        'rt_max': float(parts[7]) / 1000,
        'rt_mean': float(parts[8]) / 1000,
        'rt_std': float(parts[9]) / 1000
    }


def parse_log(filename):
    with open(filename, 'r') as infile:
        data = {
            'get_local': [],
            'set_local': [],
            'all_local': [],
            'get_global': [],
            'set_global': [],
            'all_global': [],
            'get_bucket': [0] * 32,
            'set_bucket': [0] * 32,
            'all_bucket': [0] * 32
        }

        operation = None
        final_stats_operation = None

        for line in infile:
            if not line:
                operation = None
            elif line.startswith('[1;1H[2J') or line.startswith('Type'):
                continue
            elif line.startswith('Get Statistics ('):
                final_stats_operation = OPERATION_GET
            elif line.startswith('Set Statistics ('):
                final_stats_operation = OPERATION_SET
            elif line.startswith('Total Statistics ('):
                final_stats_operation = OPERATION_ALL
            elif log2_dist_line_re.match(line):
                numbers = number_re.findall(line)
                base = int(numbers[0])
                for index, count in enumerate(numbers[1:]):
                    if final_stats_operation == OPERATION_GET:
                        data['get_bucket'][index + base] = int(count)
                    elif final_stats_operation == OPERATION_SET:
                        data['set_bucket'][index + base] = int(count)
                    elif final_stats_operation == OPERATION_ALL:
                        data['all_bucket'][index + base] = int(count)
            elif line.startswith('Get Statistics'):
                operation = OPERATION_GET
            elif line.startswith('Set Statistics'):
                operation = OPERATION_SET
            elif line.startswith('Total Statistics'):
                operation = OPERATION_ALL
            elif line.startswith('Period'):
                if operation == OPERATION_GET:
                    data['get_local'].append(_parse_operation_entry(line))
                elif operation == OPERATION_SET:
                    data['set_local'].append(_parse_operation_entry(line))
                elif operation == OPERATION_ALL:
                    data['all_local'].append(_parse_operation_entry(line))
                else:
                    raise ValueError("Operation entry without associated operation type")
            elif line.startswith('Global'):
                if operation == OPERATION_GET:
                    data['get_global'].append(_parse_operation_entry(line))
                elif operation == OPERATION_SET:
                    data['set_global'].append(_parse_operation_entry(line))
                elif operation == OPERATION_ALL:
                    data['all_global'].append(_parse_operation_entry(line))
                else:
                    raise ValueError("Operation entry without associated operation type")
            elif line.startswith('threads count:'):
                data['threads_count'] = int(number_re.findall(line)[-1])
            elif line.startswith('concurrency:'):
                data['concurrency'] = int(number_re.findall(line)[-1])
            elif line.startswith('run time:'):
                data['run_time'] = int(number_re.findall(line)[-1])
            elif line.startswith('windows size:'):
                data['windows_size'] = int(number_re.findall(line)[-1]) * 1000
            elif line.startswith('set proportion'):
                data['set_proportion'] = float(number_re.findall(line)[-1])
            elif line.startswith('get proportion'):
                data['get_proportion'] = float(number_re.findall(line)[-1])

        return data


def aggregate_data(config):
    if config['request_type'] == 'g':
        prefix = 'get'
    elif config['request_type'] == 's':
        prefix = 'set'
    elif config['request_type'] == 'a':
        prefix = 'all'
    local_key = prefix + '_local'
    global_key = prefix + '_global'
    bucket_key = prefix + '_bucket'

    start = config['start']
    duration = config['duration']
    end = start + duration
    ci_coefficient = config['ci_coefficient']
    num_repetitions = config['num_repetitions']
    num_client_vms = config['num_client_vms']
    base_dir = config['base_dir']

    tps = np.zeros(num_repetitions)
    rts = np.zeros(num_repetitions * num_client_vms * duration)
    rts_w = np.zeros(num_repetitions * num_client_vms * duration)
    rts_i = 0
    rts_b = None

    for repetition in range(1, num_repetitions + 1, 1):
        repetition_tp = 0

        for client_id in range(1, num_client_vms + 1, 1):
            filename = os.path.join(
                base_dir,
                'repetition-{}-client-{}.log'.format(repetition, client_id)
            )

            data = parse_log(filename)

            num_ops = data[global_key][end - 2]['num_ops'] - data[global_key][start - 2]['num_ops']
            repetition_tp += (num_ops // duration)

            for entry in data[local_key][start - 1: end - 1]:
                rts[rts_i] = entry['rt_mean']
                rts_w[rts_i] = entry['num_ops']
                rts_i += 1

            if rts_b is None:
                rts_b = list(data[bucket_key])
            else:
                rts_b = [x + y for x, y in zip(rts_b, list(data[bucket_key]))]

        tps[repetition - 1] = repetition_tp

    tp_mean = np.floor(np.mean(tps))
    if num_repetitions > 1:
        tp_std = np.floor(np.std(tps, ddof=1))
        tp_ci = np.floor(ci_coefficient * tp_std / np.sqrt(num_repetitions))
    else:
        tp_std = 0
        tp_ci = 0

    rt_mean = np.average(rts, weights=rts_w)
    rt_low = get_percentile(rts_b, 5)
    rt_high = get_percentile(rts_b, 95)

    return {
        'tps': tps,
        'tp_mean': tp_mean,
        'tp_std': tp_std,
        'tp_ci': tp_ci,
        'rt_mean': rt_mean,
        'rt_low': rt_low,
        'rt_high': rt_high
    }


def get_percentile(buckets, percentile):
    total = 0
    for count in buckets:
        total = total + count

    current = 0
    for index, count in enumerate(buckets):
        if count == 0:
            continue
        current = current + count
        if current / total * 100 >= percentile:
            return TIMES[index]
