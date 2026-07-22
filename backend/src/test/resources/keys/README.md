Test-only RSA keypair for JWT signing in automated tests (CI, local `mvnw test`).

Not a real secret — never used outside the test Spring context, safe to commit.
Local dev / production use their own keypair at `src/main/resources/keys/` (gitignored),
configured via `JWT_PRIVATE_KEY_PATH`/`JWT_PUBLIC_KEY_PATH`.
