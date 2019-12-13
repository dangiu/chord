import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import numpy as np
import os

from scipy.optimize import curve_fit

sns.set(color_codes=True)

if __name__ == "__main__":

    input_file = 'data/occ/OccCount.2019.dic.13.13_58_41.txt'
    nodes = 0
    count = []
    occs = []

    input = open(input_file)

    dataReached = False
    for line in input.readlines():
        if('keyCount' in line):
            dataReached = True
            continue
        if(dataReached):
            if(',' in line):
                if('patLen' in line):
                    # avoid header
                    print(line)
                else:
                    temp = line.split(',')
                    count.append(int(temp[0]))
                    occs.append(int(temp[1]))


    df = pd.DataFrame({'count':count, 'occ':occs})

    totalOcc = df['occ'].sum()

    df = df.sort_values('occ')

    df['occ'] = df['occ'].map(lambda occ: occ/totalOcc)

    ax = sns.lineplot(x='count', y='occ', data=df)

    ax.legend()
    ax.set(xlabel='# Key Stored per Node', ylabel='PDF')

    plt.tight_layout()
    plt.show()