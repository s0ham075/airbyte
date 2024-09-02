"""Simple script to download secrets from GCS.

Usage:
    pipx install uv
    uv run scripts/fetch_test_secrets.py

Inline dependency metadata for `uv`:

# /// script
# requires-python = "==3.10"
# dependencies = [
#     "airbyte",  # PyAirbyte
# ]
# ///

"""

from __future__ import annotations

from pathlib import Path

import airbyte as ab
from airbyte.secrets import GoogleGSMSecretManager, SecretHandle


AIRBYTE_INTERNAL_GCP_PROJECT = "dataline-integration-testing"


def main() -> None:
    # Get a PyAirbyte secret manager instance for the GCP project.
    secret_mgr = GoogleGSMSecretManager(
        project=AIRBYTE_INTERNAL_GCP_PROJECT,
        credentials_json=ab.get_secret("GCP_GSM_CREDENTIALS"),
    )

    # Fetch and write secrets to the `secrets/` directory, using the `filename` label
    # to name the file.
    secret: SecretHandle
    for secret in secret_mgr.fetch_connector_secrets("source-s3"):
        if "filename" in secret.labels:
            secret.write_to_file(Path(f'secrets/{secret.labels["filename"]}.json'))


if __name__ == "__main__":
    main()
