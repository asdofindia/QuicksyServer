# QuicksyServer
QuicksyServer is the backend of the [Quicksy](https://quicksy.im)-App that handles both registration of new users (verified by SMS) and phone number to Jabber ID discovery.

## HTTP API

The HTTP API is responsible for registration, password resets and SMS verification. As far as the app is concerned initial registration and password reset behave exactly the same.

### `GET /authentication/$phoneNumber`

Requests an SMS containing a 6 digit code to be send to `$phoneNumber`. The number is formatted according to E.164 (leading +, including the contry code, no spaces). For example `GET /authentication/+18005222443`.

#### Request Header

* `Accept-Language`: ISO 639-1 two letter language code. Will be used to change the language of the SMS
* `Installation-Id`: Randomly generated (at install time) UUID supplied by the client. Will be used for rate limiting on top of per number and per IP limits. Limits users on requesting multiple SMS for multiple phone numbers.
* `User-Agent`: Formatted to `Name/Version` with version being formatted according to [Sementic Versioning](https://semver.org/).

#### Response Codes

* `200`: Everything ok. SMS has been sent.
* `400`: Returned when supplied data (including headers) is invalid or not existend. This can include invalid phone number, invalid *Accept-Language*, missing or invalid *Installation-Id*. Since the phone number is already validated when entering it in the app the error will rarely be thrown. Displays to user as: **Invalid user input**.
* `403`: Outdated app version (as reported by *User-Agent*). Displays to user as: **You are using an out of date version of this app.**
* `409`: Conflict. When attempting to request an SMS for a phone number that is currently still logged in. Displays to user as: **This phone number is currently logged in with another device.**
* `429`: Rate limited. Displays to user as: **Please try again in …**
* `500`: Internal Server Error. Unable to reach the database, the XMPP server or the SMS verification provider. Displays to user as: **Something went wrong processing your request.**
* `501`, `502`, `503`: Temporary errors. Usually not throwns by QuicksyServer but by reverse proxy in front of it. Displays to user as: **Temporarily unavailable. Try again later.** Note: Not to be used for rate limiting.

#### Response Header

* `Retry-After`: Time in seconds after which the client can make another attempt. Parsed in combination with response code `429`.

### `POST /password`

## XMPP API
