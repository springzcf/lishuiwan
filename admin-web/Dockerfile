FROM node:24-alpine AS build
WORKDIR /build
RUN corepack enable
COPY admin-web/package.json admin-web/pnpm-lock.yaml admin-web/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY admin-web/ .
RUN pnpm build

FROM nginx:1.28-alpine
COPY --from=build /build/dist /usr/share/nginx/html/admin
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
