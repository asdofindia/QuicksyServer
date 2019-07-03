# QuicksyServer
QuicksyServer is the backend of the [Quicksy](https://quicksy.im)-App that handles both registration of new users (verified by SMS) and phone number to Jabber ID discovery.

## HTTP API

The HTTP API is responsible for registration, password resets and SMS verification. As far as the app is concerned initial registration and password reset behave exactly the same.

### `GET /authentication/$phoneNumber`

#### Header

#### Response Codes

### `POST /password`

## XMPP API
