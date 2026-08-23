# CMS repository instructions

## Canonical source

- Repository: `git@github.com:ChristianLeonhardtGIT/cms.git`
- Branch: `main`
- Production host: `191.218.164.219`
- Production path: `/opt/cleonhardt`
- Magnolia project: `cleonhardt`

Treat `origin/main` as the source of truth for every later CMS update. Pull it
before editing, work on a branch, verify the change locally, and deploy only
after the reviewed change has been merged.

## Boundaries

- Never commit credentials, tokens, private keys, user accounts, raw Magnolia
  repositories, beta-portal data or Caddy runtime data.
- Keep production secrets in `/opt/cleonhardt/secrets` or in the protected
  runtime environment.
- Keep full runtime/volume backups outside GitHub.
- Version only approved, non-personal editorial JCR exports under
  `content-snapshots/`.
- ChOS source content belongs to `ChristianLeonhardtGIT/ChOS`; this repository
  contains only the CMS integration and reader.

Read `docs/SOURCE_OF_TRUTH.md` and `docs/RESTORE.md` before deployment or
recovery work.
