import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import os

sns.set(color_codes=True)

if __name__ == "__main__":

    input_folder = 'data/path len'
    #nodes,avgPathLen,bestPathLen,worstPathLen
    nodes = []
    avgPathLen = []
    bestPathLen = []
    worstPathLen = []

    for file in os.listdir(input_folder):
        input = open(input_folder + '/' + file, 'r')
        lines = input.readlines()
        param = lines[3].split(',')
        nodes.append(int(param[0]))
        avgPathLen.append(float(param[1]))
        bestPathLen.append(float(param[2]))
        worstPathLen.append(float(param[3]))


    df = pd.DataFrame({'nodes':nodes, 'avgPathLen':avgPathLen, 'bestPathLen':bestPathLen, 'worstPathLen':worstPathLen})

    print(df)

    flatui = ["#9b59b6", "#3498db", "#95a5a6", "#e74c3c", "#34495e", "#2ecc71"]
    sns.set_palette(flatui)

    ax = sns.lineplot(x='nodes', y='avgPathLen', data=df, label="Average")
    ax = sns.lineplot(x='nodes', y='bestPathLen', data=df, label="Best")
    ax = sns.lineplot(x='nodes', y='worstPathLen', data=df, label="Worst")

    ax.legend()
    ax.set(xlabel='# of Nodes', ylabel='Path Length')

    plt.tight_layout()
    plt.show()