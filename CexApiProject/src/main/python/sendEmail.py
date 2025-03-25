import smtplib
from email.mime.text import MIMEText
from email.header import Header
from email.mime.multipart import MIMEMultipart
import sys

# 获取参数列表
args = sys.argv[1:]

# 设置服务器参数
smtp_server = 'smtp.163.com'
port = 465  # 或者587，取决于服务器要求
sender = 'erlongxizhu_03@163.com'#qq的'jjcsjiflyinmheib'
password = 'CJkGA34PYD86SRUP'  # 使用应用专用密码
receiver = '1593612833@qq.com'
message = MIMEMultipart()
 # 设置邮件内容
#body = '这是一封python发送的<span style="color:red;"><br>测试邮件！</span><br><span style="color:blue;">开空</span> 1BTC'
body = args[0]
message.attach(MIMEText(body, 'html'))
#message = MIMEText('这是一封python发送的\n测试邮件！\n开空1BTC', 'plain', 'utf-8')
message['From'] = Header(sender)
message['To'] = Header(receiver)
message['Subject'] = Header('达到指标，提示')


try:
    # 发送邮件
    server = smtplib.SMTP_SSL(smtp_server, port)
    server.login(sender, password)
    server.sendmail(sender, [receiver], message.as_string())
    print('sendEmail成功')
except BaseException as e:
    print(e)
finally:
    #不管是否发送成功，都退出服务
    server.quit()



