from __future__ import annotations

from typing import TYPE_CHECKING, Final

from starlette.datastructures import Headers
from starlette.middleware.trustedhost import TrustedHostMiddleware

if TYPE_CHECKING:
    from starlette.types import ASGIApp, Message, Receive, Scope, Send

_BODY_TOO_LARGE: Final = b'{"detail":"Request body too large."}'


class PathScopedTrustedHostMiddleware:
    """Validate public API hosts without breaking direct pod health and metrics scrapes."""

    def __init__(self, app: ASGIApp, *, allowed_hosts: tuple[str, ...], path_prefix: str) -> None:
        self._app = app
        self._trusted_app = TrustedHostMiddleware(app, allowed_hosts=list(allowed_hosts))
        self._path_prefix = path_prefix

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] == "http" and scope["path"].startswith(self._path_prefix):
            await self._trusted_app(scope, receive, send)
            return
        await self._app(scope, receive, send)


class RequestBodyLimitMiddleware:
    """Buffer a small bounded API body before framework parsing; never buffer SSE output."""

    def __init__(self, app: ASGIApp, *, max_body_bytes: int, path_prefix: str) -> None:
        self._app = app
        self._max_body_bytes = max_body_bytes
        self._path_prefix = path_prefix

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http" or not scope["path"].startswith(self._path_prefix):
            await self._app(scope, receive, send)
            return

        content_length = Headers(scope=scope).get("content-length")
        if content_length is not None:
            try:
                if int(content_length) > self._max_body_bytes:
                    await _send_too_large(send)
                    return
            except ValueError:
                await _send_too_large(send)
                return

        messages: list[Message] = []
        body_size = 0
        while True:
            message = await receive()
            messages.append(message)
            if message["type"] == "http.disconnect":
                return
            if message["type"] != "http.request":
                continue
            body_size += len(message.get("body", b""))
            if body_size > self._max_body_bytes:
                await _send_too_large(send)
                return
            if not message.get("more_body", False):
                break

        index = 0

        async def replay_receive() -> Message:
            nonlocal index
            if index < len(messages):
                message = messages[index]
                index += 1
                return message
            return await receive()

        await self._app(scope, replay_receive, send)


async def _send_too_large(send: Send) -> None:
    await send(
        {
            "type": "http.response.start",
            "status": 413,
            "headers": [
                (b"content-type", b"application/json"),
                (b"content-length", str(len(_BODY_TOO_LARGE)).encode("ascii")),
            ],
        }
    )
    await send({"type": "http.response.body", "body": _BODY_TOO_LARGE})
