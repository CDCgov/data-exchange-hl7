# CDC Data Exchange HL7

HL7 pipeline functions project: receiver-debatcher, redactor, structure-validator, mmg-validator, mmg-based-transformer, mmg-sql-transformer, lake-segments-transformer, json-lake-transformer

#### HL7 receiver-debatcher pipeline function - Receives HL7 data that gets uploaded to DEX and makes it available for processing via HL7 pipeline(Case notification messages and ELR)
- Code for a receiver-debatcher function -  proof-of-concept: - proof-of-concept:
[receiver-debatcher](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-receiver-debatcher)

#### HL7 redactor pipeline function - Redacts PII data from the message
- Code for a redactor function - proof-of-concept:
[redactor](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-redactor)

#### HL7 structure-validator function - validates HL7 structure using PHIN Spec profiles created in the IGAMT tool
- Code for a structure-validator function - proof-of-concept:
[structure-validator](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-structure-validator)

#### HL7 mmg-validator pipeline function - Uses MMG profiles to validate HL7 message content
- Code for a mmg-validator function - proof-of-concept:
[mmg-validator](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-mmg-validator)

#### HL7 mmg-based-transformer pipeline function
- Code for a mmg-based-transformer function - proof-of-concept:
[mmg-based-transformer](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-mmg-based-transformer)

#### HL7 mmg-sql-transformer pipeline function
- Code for a mmg-sql-transformer function - proof-of-concept:
[mmg-sql-transformer](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-mmg-sql-transformer)

#### HL7 lake-segs-transformer pipeline function - Uses HL7-PET library to parse the HL7 message(lake of segments) in a way that understands segments hierarchy
- Code for a lake-segs-transformer function - proof-of-concept:
[lake-segs-transformer](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-lake-segs-transformer)

#### HL7 json-lake pipeline function - Creates Json model based on HL7-PET profiles with segment hierarchy
- Code for a json-lake function - proof-of-concept:
[json-lake](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-hl7-json-lake)


## Public Domain Standard Notice
This repository constitutes a work of the United States Government and is not
subject to domestic copyright protection under 17 USC § 105. This repository is in
the public domain within the United States, and copyright and related rights in
the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
All contributions to this repository will be released under the CC0 dedication. By
submitting a pull request you are agreeing to comply with this waiver of
copyright interest.

## License Standard Notice
The repository utilizes code licensed under the terms of the Apache Software
License and therefore is licensed under ASL v2 or later.

This source code in this repository is free: you can redistribute it and/or modify it under
the terms of the Apache Software License version 2, or (at your option) any
later version.

This source code in this repository is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the Apache Software License for more details.

You should have received a copy of the Apache Software License along with this
program. If not, see http://www.apache.org/licenses/LICENSE-2.0.html

The source code forked from other open source projects will inherit its license.

## Privacy Standard Notice
This repository contains only non-sensitive, publicly available data and
information. All material and community participation is covered by the
[Disclaimer](https://github.com/CDCgov/template/blob/master/DISCLAIMER.md)
and [Code of Conduct](https://github.com/CDCgov/template/blob/master/code-of-conduct.md).
For more information about CDC's privacy policy, please visit [http://www.cdc.gov/other/privacy.html](https://www.cdc.gov/other/privacy.html).

## Contributing Standard Notice
Anyone is encouraged to contribute to the repository by [forking](https://help.github.com/articles/fork-a-repo)
and submitting a pull request. (If you are new to GitHub, you might start with a
[basic tutorial](https://help.github.com/articles/set-up-git).) By contributing
to this project, you grant a world-wide, royalty-free, perpetual, irrevocable,
non-exclusive, transferable license to all users under the terms of the
[Apache Software License v2](http://www.apache.org/licenses/LICENSE-2.0.html) or
later.

All comments, messages, pull requests, and other submissions received through
CDC including this GitHub page may be subject to applicable federal law, including but not limited to the Federal Records Act, and may be archived. Learn more at [http://www.cdc.gov/other/privacy.html](http://www.cdc.gov/other/privacy.html).

## Records Management Standard Notice
This repository is not a source of government records, but is a copy to increase
collaboration and collaborative potential. All government records will be
published through the [CDC web site](http://www.cdc.gov).

## Additional Standard Notices
Please refer to [CDC's Template Repository](https://github.com/CDCgov/template)
for more information about [contributing to this repository](https://github.com/CDCgov/template/blob/master/CONTRIBUTING.md),
[public domain notices and disclaimers](https://github.com/CDCgov/template/blob/master/DISCLAIMER.md),
and [code of conduct](https://github.com/CDCgov/template/blob/master/code-of-conduct.md).
