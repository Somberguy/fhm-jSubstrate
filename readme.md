# FHM-JSubstrate

![license](https://img.shields.io/badge/for-human-blue.svg)  

![license](https://img.shields.io/badge/license-apache2.0-blue.svg) ![java](https://img.shields.io/badge/java-v1.8-green.svg) ![release](https://img.shields.io/badge/release-v1.0.0-red.svg)

## 简介

`FHM-JSubstrate`是轻量级的Java基础框架，他不但兼顾了Spring中***IOC***的常用特性，还有***配置管理***、***IOC仓库规划***、***定点扫描注入***等功能。
具有对象初始化速度快、研发项目占用磁盘体积小、对象占用VM内存小的优秀特点。更重要的是其代码实现简单、阅读难度低、透明度高，
在项目开发中找寻项目BUG速度快、还可以自定义`IOC`功能，由于代码逻辑易读、透明度高，在安全方面有很好的保障。

## 背景

在项目研发中，时常会有需要对象在VM中是单例的情况，通常使用单例模式的方式处理，以求在VM中有高效的内存管理，但是这样对象数量变多就不能很好管理。
`Spring-Framework`使用`IOC`解决了此类问题，随着`Spring-Framework`迭代，其在当下有三个问题需要解决：一、随着版本迭代，因为需要兼顾早期版本的需求，
致使目前`Spring-Framework`磁盘体积愈重、启动速度愈慢。在研发中小型的程序、插件时使用`Spring-Framework`
就显得冗余；二、`Spring-Framework`实现复杂，
源码阅读困难在一些特殊场景，我们需要对`IOC`做自定义修改源码实现困难，比如现有需求：用户需要根据注入的标志注解的不同，分别对该注解注入的对象做不同处理
***IOC仓库规划***。三、针对配置文件属性注入繁琐。在此背景下，`FHM-JSubstrate`诞生了。  
（ 在个人看来`Spring-Framework`的`IOC`最大的优势是`Spring-Boot`的体系依赖于它。`Spring-Boot`
成功利用操作系统BIOS程序的思想创建了优秀、完善的框架体系。）

## 环境

_开发语言：_ `Java`  
_JDK-VERSION：_ 1.8  
_开发OS：_ `Windows`、`Macos`

## 快速开始

_请阅览_[quick-start_cn.md](quick-start_cn.md)

## 功能特性

_请阅览_[features_cn.md](features_cn.md)

## 声明

项目由`FHM`长期支持维护。  
如果遇到问题或者需要使用指导，请添加`Wechat`：`NincoTan`。

## 开发团队

`FHM` - 为了人类  
为促进人类发展而生。  
我们希望用科技改变人类的未来，实现FHM的价值，  
从而实现我们每个人的价值，让生活不再乏味，
让我们每个人都可以在这个世界上留下“我来过”的宣言。



