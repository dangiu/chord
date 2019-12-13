import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import os

sns.set(color_codes=True)

if __name__ == "__main__":

    input_folder = 'data/churn lookup'
    #initNodes,churn,avgStab,avgFixFing,fractionFailedLookups
    churn = []
    fraction = []

    for file in os.listdir(input_folder):
        input = open(input_folder + '/' + file, 'r')
        for line in input.readlines():
            if ('500' in line):
                temp = line.split(",")
                churn.append(float(temp[1]))
                fraction.append(float(temp[4]))

    df = pd.DataFrame({'churn':churn, 'fraction':fraction})


    flatui = ["#9b59b6", "#3498db", "#95a5a6", "#e74c3c", "#34495e", "#2ecc71"]
    sns.set_palette(flatui)

    ax = sns.lineplot(x='churn', y='fraction', data=df)
    #ax = sns.lineplot(x='nodes', y='bestPathLen', data=df, label="Best")
    #ax = sns.lineplot(x='nodes', y='worstPathLen', data=df, label="Worst")

    ax.set(xlabel='Churn Rate', ylabel='% of Lookups Failed')

    plt.tight_layout()
    plt.show()
