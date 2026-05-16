FROM node:20-alpine AS frontend-build

WORKDIR /app

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./

ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build

FROM node:20-alpine AS admin-build

WORKDIR /app

ENV NODE_OPTIONS=--openssl-legacy-provider

COPY admin-frontend/package*.json ./
RUN npm ci

COPY admin-frontend/ ./
RUN npm run build:prod

FROM nginx:1.27-alpine

ENV BACKEND_HOST=backend
ENV BACKEND_PORT=8080

COPY nginx/templates/ /etc/nginx/templates/
COPY --from=frontend-build /app/dist/ /usr/share/nginx/html/
COPY --from=admin-build /app/dist/ /usr/share/nginx/html/admin/

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/healthz || exit 1
