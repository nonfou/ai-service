# SSL 证书目录

将你的 SSL 证书文件放置在此目录：

- `fullchain.pem` - 证书链（包含服务器证书和中间证书）
- `privkey.pem` - 私钥文件

## 获取证书

### 方式一：Let's Encrypt (免费)
```bash
# 使用 certbot 获取证书
certbot certonly --standalone -d yourdomain.com

# 证书位置
# /etc/letsencrypt/live/yourdomain.com/fullchain.pem
# /etc/letsencrypt/live/yourdomain.com/privkey.pem
```

### 方式二：自签名证书 (仅用于测试)
```bash
# 生成自签名证书
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout privkey.pem \
  -out fullchain.pem \
  -subj "/CN=localhost"
```

## 注意事项

- 确保证书文件权限正确（建议 644）
- 私钥文件应妥善保管，不要提交到版本控制
- 生产环境建议使用受信任的 CA 颁发的证书
