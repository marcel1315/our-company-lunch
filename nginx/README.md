# EC2 Amazon Linux 2023에서 nginx 설정

- nginx 설치
  ```
  sudo dnf install nginx -y
  ```

- nginx 시작 및 서비스 등록
  ```
  sudo systemctl enable nginx
  sudo systemctl start nginx
  ```

- nginx 설정 추가. 해당 디렉토리에 있는 파일의 내용을 붙여넣음
  ```
  cd /etc/nginx/conf.d
  sudo vi ourcompanylunch.conf
  ```

- nginx 설정 테스트
  ```
  nginx -t
  ```

# Mac 에서 nginx 설정

- nginx 설정 추가. 해당 디렉토리에 있는 파일의 내용을 붙여넣음
  ```
  cd /opt/homebrew/etc/nginx/servers/
  sudo vi ourcompanylunch.conf  
  ```

- nginx 설정 테스트
  ```
  sudo nginx -t
  ```

- nginx 재시작
  ```
  sudo nginx -s reload
  ```