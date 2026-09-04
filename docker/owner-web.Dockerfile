FROM node:20-alpine AS builder
WORKDIR /build
COPY ../property-owner-web/package*.json ./
RUN npm ci
COPY ../property-owner-web/. .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY docker/nginx/owner.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
