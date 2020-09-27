这是一个基于java底层的工具包,功能包括热部署、调用链跟踪等。**dwow 意为 do whatever one wants(随心所欲)**

## 热部署
基于java agent、javassist技术实现，采用自定义ClassLoader加载需要热部署的类

## 调用链跟踪
基于java agent、javassist、jvm attach实现的插桩技术

## 参考文档
- https://cloud.tencent.com/developer/article/1694162