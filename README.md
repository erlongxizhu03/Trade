version1：lnexchange获取对应bitcoin价格。一定时间内波动率指标、固定价格指标，提示

pom 中py文件目录，生成到和jar包同一目录下一份，就在jar运行中也能找到对应py文件。
打jar包：
F:\CexApiProject>项目目录下，打包jar命令：
mvn package assembly:single
jar包目录下F:\CexApiProject\target，然后设置编码并运行
chcp 65001
java  -jar xxx.jar运行
