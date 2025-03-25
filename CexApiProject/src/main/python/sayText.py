# 在这里写上你的代码 :-)
import pyttsx3
import sys

# 获取参数列表
args = sys.argv[1:]

def say(text):
    engine = pyttsx3.init()
    engine.say(text)
    engine.runAndWait()

say(args[0])