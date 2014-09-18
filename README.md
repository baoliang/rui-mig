# 中瑞财富数据变更角本模块

## 数据库结构变更与脚本运行模块使用说明
*安装条件 安装rui-db库 在 http://git.rui/libs/rui_mig_db/tree/master/rui-db 下面执行lein install
*安装
 添加 以下代码到lein 项目的project.clj
 ```
 :plugins [[rui-mig "0.1.0-SNAPSHOT"]]
 ```

* 添加脚本文件：执行lein rui-mig create 生成一个src/rui/migrations/m20140310xxxx.clj 的文件里面在up函数里面写入要执行代码

* 运行脚本：在lein项目里面执行 lein rui-mig

* 部署成单个运行文件 lein rui-mig deploy 生成xxx.main.clj

* 部署jar 运行migration 在项目的main代码里引入xxx.main.clj 打包成jar 运行 java -cp xxx.jar xxx.main

* 特别注意的是如果执行函数的时侯一定要注意异常的抛出。如果处理了异常而不抛出的话会认为此脚本运行成功。以后不再执行此脚本。



