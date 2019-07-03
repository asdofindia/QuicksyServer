# QuicksyServer
QuicksyServer is the backend of the [Quicksy](https://quicksy.im)-App that handles both registration of new users (verified by SMS) and phone number to Jabber ID discovery.

## HTTP API

The HTTP API is responsible for registration, password resets and SMS verification. As far as the app is concerned initial registration and password reset behave exactly the same.

### `GET /authentication/$phoneNumber`

Requests an SMS containing a 6 digit code to be send to `$phoneNumber`. The number is formatted according to E.164 (leading +, including the contry code, no spaces).

#### Header

* `Accept-Language`: ISO 639-1 two letter language code. Will be used to change the language of the SMS
* `Installation-Id`: Randomly generated (at install time) UUID supplied by the client. Will be used for rate limiting on top of per number and per IP limits. Limits users on requesting multiple SMS for multiple phone numbers.
* `User-Agent`: Formatted to `Name/Version` with version being formatted according to [Sementic Versioning](https://semver.org/).

#### Response Codes

### `POST /password`

## XMPP API
