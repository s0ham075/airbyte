# Copyright (c) 2024 Airbyte, Inc., all rights reserved.
from datetime import timedelta
from enum import Enum
from typing import Optional

from airbyte_cdk.sources.declarative.async_job.timer import Timer

class AsyncJobStatus(Enum):
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    TIMED_OUT = "TIMED_OUT"


class AsyncJob:
    """
    Note that the timer will only stop once `update_status` is called so the job might be completed on the API side but until we query for
    it and call `ApiJob.update_status`, `ApiJob.status` will not reflect the actual API side status.
    """
    def __init__(self, api_job_id: str, timeout: Optional[timedelta] = None) -> None:
        self._api_job_id = api_job_id
        self._status = AsyncJobStatus.RUNNING

        timeout = timeout if timeout else timedelta(minutes=60)
        self._timer = Timer(timeout)
        self._timer.start()

    def api_job_id(self) -> str:
        return self._api_job_id

    def status(self) -> AsyncJobStatus:
        if self._timer.has_timed_out():
            return AsyncJobStatus.TIMED_OUT
        return self._status

    def update_status(self, status: AsyncJobStatus) -> None:
        if self._status != AsyncJobStatus.RUNNING and status == AsyncJobStatus.RUNNING:
            self._timer.start()
        elif status in [AsyncJobStatus.FAILED, AsyncJobStatus.TIMED_OUT, AsyncJobStatus.COMPLETED]:
            self._timer.stop()

        self._status = status
