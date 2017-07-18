import csv
import operator
import os

import numpy as np


def exam_log(filename, level='D', output=True):
    with open(filename, 'r') as infile:
        reader = csv.reader(infile, delimiter=',')
        found = False
        for row in reader:
            if row[1] == level:
                found = True
                if output:
                    print(row)

        if not found:
            print("No log entries for the specified level {}".format(level))


def parse_log(filename, request_type='a'):
    entries = []
    with open(filename, 'r') as infile:
        reader = csv.reader(infile, delimiter=',')
        for row in reader:
            if row[1] != 'I':
                continue
            if request_type != 'a' and request_type != row[3]:
                continue
            # Completion time converted to second
            row[2] = int(row[2]) // 1000
            # Transaction ID
            row[4] = int(row[4])
            # Server ID
            row[5] = int(row[5])
            # Success flag
            if row[6] == 's':
                row[6] = True
            else:
                row[6] = False
            # Cache hit flag
            if row[7] == 'h':
                row[7] = True
            else:
                row[7] = False
            # Time to read request
            row[8] = int(row[8])
            # Time in the queue
            row[9] = int(row[9])
            # Time to send request
            row[10] = int(row[10])
            # Time to receive response
            row[11] = int(row[11])
            # Time to send response
            row[12] = int(row[12])

            entries.append(row)

    return sorted(entries, key=operator.itemgetter(2))


def count_client_connection_time(filename):
    count = 0
    start = 1479931449128
    end = 0
    with open(filename, 'r') as infile:
        reader = csv.reader(infile, delimiter=',')
        for row in reader:
            if row[1] != 'D':
                continue
            if row[-1] != 'Middleware accepted new connection from client':
                continue

            time = int(row[2]) // 1000
            start = min(start, time)
            end = max(end, time)

            count += 1

    return count, end - start + 1


def aggregate_throughput(entries, interval=5, sample_frequency=100, request_type='a'):
    tps = []
    next_time = None
    for row in entries:
        if not row[6]:
            continue

        if request_type != 'a' and request_type != row[3]:
            continue

        if next_time is None:
            next_time = row[2] + interval

        if row[2] < next_time:
            if len(tps) == 0:
                tps.append(1)
            else:
                tps[-1] += 1
        else:
            tps.append(1)
            next_time += interval

    return [tp * sample_frequency for tp in tps]


def count_runtime(entries, request_type='a'):
    start_time = None
    end_time = None
    for row in entries:
        if request_type != 'a' and request_type != row[3]:
            continue

        if start_time is None:
            start_time = row[2]

        end_time = row[2]

    return end_time - start_time + 1


def aggregate_data(config):
    base_dir = config['base_dir']
    num_repetitions = config['num_repetitions']
    start = config['start']
    duration = config['duration']
    request_type = config['request_type']

    read_request_times = []
    queue_times = []
    forward_request_times = []
    server_times = []
    send_response_times = []
    total_times = []

    for repetition in range(1, num_repetitions + 1, 1):
        filename = os.path.join(
            base_dir,
            'repetition-{}-trace.log'.format(repetition)
        )
        data = parse_log(filename, request_type=request_type)
        start_time = data[0][2] + start
        end_time = start_time + duration
        for entry in data:
            if entry[2] < start_time:
                continue
            if entry[2] >= end_time:
                break
            if entry[6] is False:
                continue

            read_request_times.append(entry[8])
            queue_times.append(entry[9])
            forward_request_times.append(entry[10])
            server_times.append(entry[11])
            send_response_times.append(entry[12])
            total_times.append(entry[8] + entry[9] + entry[10] + entry[11] + entry[12])

    return {
        'read_request': np.mean(read_request_times) / 1000000,
        'queue': np.mean(queue_times) / 1000000,
        'forward_request': np.mean(forward_request_times) / 1000000,
        'server': np.mean(server_times) / 1000000,
        'send_response': np.mean(send_response_times) / 1000000,
        'total': np.mean(total_times) / 1000000,
    }
