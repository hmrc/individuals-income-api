This API allows government departments to get information from HM Revenue and Customs (HMRC) about an individual’s income. This includes income reported:

- through the PAYE process
- in a Self Assessment tax return

Data will be filtered using scopes so that only relevant data is shared. We’ll assign scopes
based on your data requirements.

The responses exclude fields when they do not have a value.

This API is a HAL HATEOAS RESTful API. It has been designed to promote discoverability and to be self documenting.

A HATEOAS API makes it clear to client software what further actions are available when an action is completed. Responses from an endpoint include URLs to further endpoints you can call. New functionality can be added without breaking your client software.

This API is still under development and further enhancements are planned. We recommend following the HATEOAS approach from the start, so that your work is not affected by future changes.

Follow URLs as they are presented to you in the API at runtime. This will prevent you from building state into your client, and will decouple you from changes to the API.

The default Media Type for responses is hal+json. 
