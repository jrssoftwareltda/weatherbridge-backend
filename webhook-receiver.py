from http.server import BaseHTTPRequestHandler, HTTPServer
import json


class WebhookHandler(BaseHTTPRequestHandler):

    def do_POST(self) -> None:
        content_length = int(
            self.headers.get("Content-Length", "0")
        )

        raw_body = self.rfile.read(
            content_length
        ).decode("utf-8")

        print()
        print("=== WeatherBridge webhook received ===")
        print(f"Path: {self.path}")
        print(
            "Content-Type:",
            self.headers.get("Content-Type")
        )
        print(
            "Correlation ID:",
            self.headers.get("X-Correlation-ID")
        )

        try:
            payload = json.loads(raw_body)
            print(
                json.dumps(
                    payload,
                    indent=2,
                    ensure_ascii=False
                )
            )
        except json.JSONDecodeError:
            print(raw_body)

        print("======================================")
        print()

        self.send_response(204)
        self.end_headers()

    def log_message(
            self,
            format: str,
            *args
    ) -> None:
        return


if __name__ == "__main__":
    server = HTTPServer(
        ("0.0.0.0", 9090),
        WebhookHandler
    )

    print(
        "Webhook receiver listening at "
        "http://localhost:9090/alerts"
    )

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping webhook receiver...")
        server.server_close()