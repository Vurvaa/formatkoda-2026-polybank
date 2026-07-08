FROM node:22-alpine AS builder
WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

FROM nginx:1.27-alpine
RUN apk add --no-cache libcap \
    && addgroup -S webapp && adduser -S webapp -G webapp \
    && chown -R webapp:webapp /usr/share/nginx/html /var/cache/nginx /var/log/nginx /etc/nginx/conf.d \
    && touch /var/run/nginx.pid \
    && chown webapp:webapp /var/run/nginx.pid \
    && setcap 'cap_net_bind_service=+ep' /usr/sbin/nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /build/dist /usr/share/nginx/html

USER webapp
EXPOSE 80