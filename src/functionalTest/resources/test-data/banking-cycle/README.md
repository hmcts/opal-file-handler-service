# DWP and Bailiffs invalid-file checks

The retained local pack has four examples: malformed DWP, Jacobs, CDER and Marston input. The twelve
positive/recovery/replay examples are removed from this implementation. Developer defect
[PO-11480](https://hmcts.atlassian.net/browse/PO-11480) records the observed downstream contract issues
and references existing BU-ID migration and allocation work, rather than duplicating database stories.

Run using the existing functional runner:

```shell
./gradlew functional '-Dcucumber.filter.tags=@ConnectedBanking'
```

| Retained check | Reuse / assertion |
| --- | --- |
| Rejected source | Existing metadata polling requires SOURCE/FAILED and the expected XML error |
| Retained bytes | Existing blob client verifies complete malformed bytes and checksum |
| Content rejection | Existing API client requires HTTP 422 for the failed source |
| No downstream output | One source record only; no Fines job/file for the owned BU/filename |
| BAIS retention | Existing SFTP client verifies the malformed input remains available |
| Setup/teardown | Owned SQL fixtures, collision guards, SftpRetry and independent cleanup error aggregation |

Each supplier folder contains only `fines-setup.sql`, `handler-setup.sql` and `fines-cleanup.sql`.
The input is the existing shared `test-data/bais/invalid-content.txt`; no Desktop path or manual file
upload is used. The Fines SQL creates only the owned BU, without accounts/APRs. BUs 7240–7243 and
owned filenames are isolated; an unexpected queued consumer reference is retained for diagnosis.

Do not run alongside other ingestion against the same SFTP folders. Known baseline input is
byte-verified before temporary removal and restored after the case. Never purge a shared queue or
bypass fixture collision guards to force a pass.

Existing 64 shared integration cases, eight enhanced functional paths and 24 persistent DWP/Bailiff
input cases remain separate coverage. Passing these four rejection cases does not prove the removed
connected journey, final allocation or resolution of PO-11480.
