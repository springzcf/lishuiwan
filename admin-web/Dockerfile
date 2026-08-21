ARG NODE_BASE_IMAGE=node:24-alpine
ARG NGINX_BASE_IMAGE=nginx:1.28-alpine

FROM ${NODE_BASE_IMAGE} AS build
WORKDIR /build
RUN corepack enable
COPY admin-web/package.json admin-web/pnpm-lock.yaml admin-web/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY admin-web/ .
RUN pnpm build

FROM ${NODE_BASE_IMAGE} AS h5-build
WORKDIR /build
COPY h5/package.json h5/package-lock.json ./
RUN npm ci
COPY h5/ .
RUN npm run build

FROM ${NGINX_BASE_IMAGE}
COPY --from=build /build/dist /usr/share/nginx/html/admin
COPY --from=h5-build /build/dist /usr/share/nginx/html/h5
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
