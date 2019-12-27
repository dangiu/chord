import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import numpy as np
import os

from scipy.optimize import curve_fit

sns.set(color_codes=True)

if __name__ == "__main__":

    input_file = 'data/path len occ/PathLenOcc.2019.dic.12.11_16_12.txt'
    nodes = 0
    lens = []
    occs = []

    input = open(input_file)

    for line in input.readlines():
        if('nodes' in line):
            temp = line.split('=')
            nodes = int(temp[1])
        if(',' in line):
            if('patLen' in line):
                # avoid header
                print(line)
            else:
                temp = line.split(',')
                lens.append(int(temp[0]))
                occs.append(int(temp[1]))


    df = pd.DataFrame({'len':lens, 'occ':occs})

    totalOcc = df['occ'].sum()

    #df['occ'] = df['occ'].map(lambda occ: occ/totalOcc)

    print(df)

    # plot data
    ax = plt.scatter(df["len"].values, df["occ"].values, label="data")

    # Fitting
    model = lambda x, A, x0, sigma, offset: offset + A * np.exp(-((x - x0) / sigma) ** 2)
    popt, pcov = curve_fit(model, df["len"].values, df["occ"].values, p0=[1, 0, 2, 0])

    # plot fit
    x = np.linspace(df["len"].values.min(), df["len"].values.max(), 250)
    plt.plot(x, model(x, *popt), label="fit")
    plt.xlabel('Path Length')
    plt.ylabel('Occurrences')
    plt.xlim(None, 10)
    plt.show()