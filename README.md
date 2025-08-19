📧 O que é o MailNotify?

O MailNotify é uma API REST que serve como um sistema de envio de notificações por e-mail, construída com Spring Boot.
Ele pode ser usado, por exemplo, em qualquer aplicação que precise disparar e-mails de forma segura, como:

Recuperação de senha (reset password)
Envio de mensagens personalizadas (boas-vindas, avisos, notificações do sistema)
Histórico de e-mails enviados para auditoria
Retentativa de envio caso o primeiro falhe
⚙️ O que o sistema oferece?

✅ Autenticação com JWT — só usuários logados conseguem enviar e-mails.
✅ Cadastro e login de usuários (com papéis ROLE_USER e ROLE_ADMIN).
✅ Envio de e-mails:

Reset de senha (e-mail padrão)

E-mail customizado (assunto + corpo definidos pelo usuário)
✅ Registro de logs — cada envio é salvo no banco (quem enviou, para quem, assunto, status, erro, data/hora).
✅ Retentativa automática (caso o servidor SMTP falhe).
✅ Controle de permissões:

Usuário comum → pode enviar e-mails

Administrador → além disso, pode ver histórico e reenviar falhas


# 📧 MailNotify — API de Notificações por E-mail (Spring Boot)

![Java](https://img.shields.io/badge/Java-21-red?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-green?logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8-blue?logo=mysql)
![JWT](https://img.shields.io/badge/Security-JWT-orange?logo=jsonwebtokens)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

API REST para **envio de notificações por e-mail** (reset de senha e mensagens customizadas).  
Conta com **autenticação JWT**, registro/login de usuários, log de envios e regras de autorização (**ROLE_USER / ROLE_ADMIN**).

---

## 🚀 Tecnologias

- Java 21
- Spring Boot 3
- Spring Security (JWT)
- Spring Data JPA + MySQL
- Spring Mail
- Spring Retry
- Maven

---

## 📑 Sumário

- [Funcionalidades](#-funcionalidades)  
- [Arquitetura](#-arquitetura-visão-rápida)  
- [Pré-requisitos](#-pré-requisitos)  
- [Configuração](#-configuração)  
- [Gerando o segredo do JWT](#-gerando-o-segredo-do-jwt)  
- [Como rodar](#-como-rodar)  
- [Endpoints](#-endpoints)  
- [Postman](#-postman-coleção-e-variáveis)  
- [Perfis e Autorização](#-perfisperfisautorização)  
- [Produção](#-empacotar-para-produção)  
- [Troubleshooting](#-dicas--troubleshooting)  
- [Licença](#-licença)  

---

## ✅ Funcionalidades

- Registro e login com JWT.  
- Envio de e-mail de **reset de senha**.  
- Envio de **mensagens customizadas** (assunto + conteúdo).  
- Logs de envio (**EmailLog**) com status, erros e data/hora.  
- Controle de acesso:
  - Público → `/api/auth/**`, `GET /actuator/health`  
  - Autenticado → envio de e-mails  
  - ADMIN → histórico e retentativa de envio  
- CORS liberado para `http://localhost:3000` (ajustável).  
- Retry automático em falhas de envio.  

---

## 🏗 Arquitetura (visão rápida)

- **SecurityConfig** → Configura JWT, CORS e permissões.  
- **JwtFilter / JwtUtil** → Valida token.  
- **UsuarioService / UsuarioRepository** → Cadastro + autenticação.  
- **EmailService / EmailController** → Reset de senha, custom, histórico, retry.  
- **EmailLog (JPA)** → Entidade que registra cada envio.  

---

## 🔧 Pré-requisitos

- Java 21  
- Maven 3.9+  
- MySQL 8  
- Conta SMTP (Gmail App Password, Mailtrap, SES etc.)  
- (Opcional) Postman  

---

## ⚙️ Configuração

Defina variáveis de ambiente:

| Variável | Exemplo | Obrigatória | Descrição |
|----------|---------|-------------|-----------|
| `JWT_SECRET` | (Base64 de 32 bytes) | ✅ | Segredo JWT |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/mailnotify?...` | ✅ | Conexão MySQL |
| `SPRING_DATASOURCE_USERNAME` | root | ✅ | Usuário MySQL |
| `SPRING_DATASOURCE_PASSWORD` | senha | ✅ | Senha MySQL |
| `SPRING_MAIL_HOST` | smtp.gmail.com | ✅ | Host SMTP |
| `SPRING_MAIL_PORT` | 587 | ✅ | Porta SMTP |
| `SPRING_MAIL_USERNAME` | seu.email@gmail.com | ✅ | Usuário SMTP |
| `SPRING_MAIL_PASSWORD` | app-password | ✅ | Senha/token SMTP |

📌 Exemplo em `application.properties` já incluído no repositório.  

---

## 🔑 Gerando o segredo do JWT

**Windows (PowerShell)**  
```powershell
$bytes = [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes(32)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
