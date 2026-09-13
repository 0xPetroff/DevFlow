# syntax=docker/dockerfile:1

FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci

COPY . .

# Baked into the bundle at build time, so the image is tied to one API location. The relative
# default is served by nginx in this same image, which is what the compose stack uses.
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

# Only the public demo sets this. It puts the seeded credentials on the login page, so a real
# installation must leave it unset.
ARG VITE_DEMO_MODE
ENV VITE_DEMO_MODE=${VITE_DEMO_MODE}

# `npm run build` type-checks first: a release should not be buildable with a type error.
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.30-alpine AS runtime

# The unprivileged image already runs as nginx (101) and listens on 8080, so no capability to
# bind a privileged port is needed anywhere in the chain.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.inc /etc/nginx/conf.d/security-headers.inc
COPY --from=build /app/dist /usr/share/nginx/html

USER 101:101
EXPOSE 8080
