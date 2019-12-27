import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

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

    print(valuesLost.head(3))
    print(valuesLost.columns)

    newDf = valuesLost
    print(newDf)
    print(newDf.size)

    ax = sns.regplot(x=newDf["runIndex"], y=newDf["lostValues"], scatter_kws={'alpha':0.1, 's':2}, color= sns.xkcd_rgb["pale red"], fit_reg=False)

    ax.set(xlabel='Run Id', ylabel='# Lost Values')

    plt.tight_layout()
    plt.show()


if __name__ == "__main__":

    input_names = [
        'data/values lost/ValuesLost.2019.dic.11.13_14_45 percentage 50.txt'
    ]
    output_names = ['temp.pkl']

    parseFile(input_names[0], output_names[0])

    computeValuesLost('temp.pkl')