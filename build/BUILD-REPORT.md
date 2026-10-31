# Build report: PASS

Whole-repository build check by Diocle (`contracts/build-report.schema.json` v0.1). Each program is certified on its own; this checks them together.

## Programs compiled together

| Program | Java files | Classes | Alone |
|---|---|---|---|
| QVFEE020 | 1 | Qvfee020 | ok |
| QVINT010 | 1 | Qvint010 | ok |
| QVLON030 | 1 | Qvlon030 | ok |

All 3 source file(s) in one javac run: **ok** (exit 0)

## CALLs (0 static CALL statement(s) examined)

- no mismatch found among the checked arguments

## Dependency graph

13 nodes, 12 edges (full graph in build-report.json).

## What was checked

- 3 program?s?: each compiled alone and all 3 Java file?s? compiled together with javac -proc:none
- top-level class names across programs
- 5 copybook?s? for drift between files ?text and analyzer layout?
- 0 static CALL statement?s?: target present, USING count and argument lengths where both sides are plain items

## What was NOT checked

- behaviour of the programs together: each program is verified on its own by differential testing against the original COBOL
- that the programs' files and datasets line up at run time ?JCL step order, DD wiring?
- CALL USING arguments that are subscripted, computed, BY VALUE or outside WORKING-STORAGE/LINKAGE items
- dynamic CALLs and programs outside this repository
