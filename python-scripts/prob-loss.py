import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import os

sns.set(color_codes=True)


def parseFile(file_name, df_name):
    input = open(file_name, 'r')
    output = open(df_name, 'w+')
    for line in input:
        if('"' in line):
            print('parsing')
            #do nothing
        else:
            output.write(line)
    input.close()
    output.close()

def computeValuesLost(file_name):
    valuesLost = pd.read_csv(file_name)

    #lv = valuesLost.iloc[:, 1].unique().tolist()
    #lv.sort()

    #lvo = pd.DataFrame({'lostValues': lv});
    #lvo['count'] =

    count = valuesLost.iloc[:, 1].value_counts()

    print(valuesLost.head(3))
    print(valuesLost.columns)

    #PROBLEM, THE NAME OF THE COLUMN BEGINS WIth A SPACEEEEE FIX THIS SHIT

    #newDf = valuesLost.drop_duplicates(subset="lostValues", keep='first')
    newDf = valuesLost
    #newDf = newDf.head(1000)
    print(newDf)

    #print(count)
    #print(count.index)
    #print(count.values)
    print(newDf.size)

    #average = valuesLost.iloc[:, 1].mean()

    #print(valuesLost[:4])
    #ax = sns.distplot(valuesLost.iloc[:, 1])
    ax = sns.regplot(x=newDf["runIndex"], y=newDf["lostValues"], scatter_kws={'alpha':0.1, 's':2}, color='red', fit_reg=False)
    #ax.legend()
    ax.set(xlabel='run id', ylabel='# lost values')

    plt.tight_layout()
    plt.show()


if __name__ == "__main__":

    input_folder = 'data/prob loss'
    nodes = []
    failedPercentage = []
    succSize = []
    probOfLoss = []

    for file in os.listdir('data/prob loss'):
        input = open(input_folder + '/' + file, 'r')
        lines = input.readlines()
        param = lines[3].split(',')
        nodes.append(int(param[0]))
        failedPercentage.append(int(param[1]))
        succSize.append(int(param[2]))
        probOfLoss.append(float(param[3]))


    df = pd.DataFrame({'nodes':nodes, 'failedPercentage':failedPercentage, 'succSize':succSize, 'probOfLoss':probOfLoss})
    sortedDf = df.sort_values('failedPercentage')

    flatui = ["#34495e", "#9b59b6", "#3498db", "#95a5a6", "#e74c3c", "#2ecc71"]
    sns.set_palette(flatui)

    ax = sns.lineplot(x='failedPercentage', y='probOfLoss', data=sortedDf)
    ax.set(xlabel='% of node crashed', ylabel='Probability of Data Loss')

    plt.tight_layout()
    plt.show()