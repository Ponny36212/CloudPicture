# 多功能可协同云相册后端

![许可证](https://img.shields.io/badge/license-MIT-blue.svg)
![JDK](https://img.shields.io/badge/JDK-17-green.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x-brightgreen.svg)

为[多功能可协同云相册前端](https://github.com/Liner03/cloudpic_frontend)提供后端服务。

## 📖 项目简介

多功能可协同云相册是一个创新的图片管理与分享平台，具有以下特色功能：

- **多空间管理**：支持公共空间、私人空间和协作工作空间
- **协同创作**：邀请他人共同管理相册内容
- **AI增强**：内置AI图像处理功能，支持图像扩展等操作
- **智能采集**：管理员可通过关键词自动采集网络图片资源

## 🚀 技术栈

- **基础框架**：Java 17 / Spring Boot 2.x
- **持久层**：MySQL / MyBatis-Plus
- **缓存方案**：Redis + Caffeine 多级缓存
- **安全框架**：Sa-Token
- **文件存储**：腾讯云COS
- **API文档**：Knife4j

## ⚙️ 开发环境

- JDK 17
- Maven 3.9
- MySQL 8
- Redis 5.0


## 📚 API文档

启动项目后，访问以下地址查看API文档：

- Knife4j: http://localhost:8081/api/doc.html

## 🔗 核心功能

### 用户管理
- 用户注册、登录、注销
- 个人信息维护
- 权限控制

### 空间管理
- 公共空间：所有用户可访问
- 私人空间：仅创建者可访问
- 工作空间：支持多人协作，权限可配置

### 图片处理
- 图片上传、下载、分享
- 图片分类、标签管理
- AI图像处理（扩图、优化等）
- 批量图片导入导出

### 智能采集
- 关键词采集网络图片（仅管理员）


## 🔗 相关链接
- [前端项目仓库](https://github.com/Liner03/cloudpic_frontend)
