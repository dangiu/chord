import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import os

sns.set(color_codes=True)

if __name__ == "__main__":

    input_folder = 'data/join'
    startTick = []
    endTick = []
    tickDict = {}
    for i in range(200, 1001):
        tickDict[i] = []


    for file in os.listdir(input_folder):
        input = open(input_folder + '/' + file, 'r')
        dataReached = False;
        for line in input.readlines():
            if ('joinStartTick' in line):
                temp = line.split('=')
                startTick.append(float(temp[1]))
            if ('joinEndTick' in line):
                temp = line.split('=')
                endTick.append(float(temp[1]))
            if('tick,count' in line):
                dataReached = True;
                continue
            if(dataReached):
                if('"' in line):
                    print("EOF reached")
                else:
                    temp = line.split(",")
                    tick = float(temp[0])
                    count = int(temp[1])
                    currEntry = tickDict[tick]
                    currEntry.append(count)
                    tickDict[tick] = currEntry;
                    #print(tick, " ", count)

    #compute average start tick
    avgStart = 0;
    for i in range(len(startTick)):
        avgStart += startTick[i]
    avgStart /= len(startTick)
    avgStart -= 200 # shift by 200 ticks
    print(avgStart)

    # compute average end tick
    avgEnd = 0;
    for i in range(len(endTick)):
        avgEnd += endTick[i]
    avgEnd /= len(endTick)
    avgEnd -= 200  # shift by 200 ticks
    print(avgEnd)

    # compute average knowledge
    avgKnowledge =[]
    for i in range(200, 1001):
        tempAvg = 0
        for j in range(len(tickDict[i])):
            tempAvg += tickDict[i][j]
        if len(tickDict[i]) != 0:
            tempAvg /= len(tickDict[i])
        avgKnowledge.append(tempAvg)

    avgKnowledge.sort()

    df = pd.DataFrame({'tick':range(len(avgKnowledge)), 'avgKnowledge':avgKnowledge})

    df = df.head(600)

    flatui = ["#9b59b6", "#3498db", "#95a5a6", "#e74c3c", "#34495e", "#2ecc71"]
    sns.set_palette(flatui)

    ax = sns.lineplot(x='tick', y='avgKnowledge', data=df)
    #ax = sns.lineplot(x='nodes', y='bestPathLen', data=df, label="Best")
    #ax = sns.lineplot(x='nodes', y='worstPathLen', data=df, label="Worst")

    ax.legend()
    ax.set(xlabel='Ticks', ylabel='Average # Aknowledged Nodes')

    plt.tight_layout()
    plt.show()
