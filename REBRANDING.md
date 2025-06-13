# AzhagiKeys Rebranding Guide

This document outlines the steps needed to complete the rebranding of FlorisBoard to AzhagiKeys.

## Completed Changes

1. Updated project name in settings.gradle.kts
2. Updated namespace in app/build.gradle.kts
3. Updated copyright headers
4. Created new package structure
5. Updated XML files (method.xml, spellchecker.xml)
6. Updated README.md with new branding
7. Created new database schema directories
8. Updated Python utility scripts

## Remaining Tasks

1. Run the renaming_script/rename_floris_to_azhagi.ps1 script to copy and transform all source files
2. Update all import statements in the codebase
3. Test the application after rebranding to ensure all functionality works
4. Update app store listings and promotional materials
5. Create new icons and branding assets

## Package Structure Changes

- Old package: `dev.patrickgold.florisboard`
- New package: `com.azhagi.azhagikeys`

## Class Renaming Pattern

- `FlorisBoard` → `AzhagiKeys`
- `Floris` prefix → `Azhagi` prefix

## Important Notes

- After running the rebranding script, a thorough testing is necessary to ensure all functionality works correctly
- Any hard-coded references to the old package name or class names should be manually updated
- The database schemas must be properly migrated to prevent data loss

## Contact

For any questions regarding the rebranding process, please contact the development team.
