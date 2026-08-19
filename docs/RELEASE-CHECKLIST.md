# Release Checklist

1. Start from a clean, reviewed commit in the authoritative root repository.
2. Confirm `./scripts/security-audit.sh` passes.
3. Confirm `./checker.sh` passes from recreated dependencies.
4. Confirm the continuous quality workflow passes on every supported platform.
5. Review user-data migrations, rollback behavior, and known limitations.
6. Run `./scripts/package.sh` and verify every checksum against its archive.
7. Inspect provenance and reject any artifact whose revision is `unversioned`.
8. Sign approved artifacts and publish the archive, checksum, provenance, notes,
   support policy, and security-contact information together.
9. Install the released artifact on a clean test account and complete a smoke test.
10. Preserve the release inputs and record rollback instructions.
