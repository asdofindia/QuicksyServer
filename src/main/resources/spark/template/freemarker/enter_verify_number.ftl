<#ftl output_format="HTML">
<#import "enter.ftl" as enter/>
<@enter.enter title="Verify your phone number">
<#if method == "SMS">
<p>We have sent you an SMS to ${number} containing a verification code.</p>
<#else>
<p>You should shorty receive a phone call to ${number} with a verification code.</p>
</#if>
<#if error??>
  <p class="error">
    <#switch error>
      <#case "invalid">
      The code you have entered is invalid.
      <#break>
      <#case "expired">
      Your verification code has expired. Click <a href="/enter/reset/?method=${method}">here</a> to start over.
      <#break>
      <#case "upstream">
      There has been a problem with our verification provider.
      <#case "attempts">
      You have made too many attempts verifying your phone number.
      <#break>
      <#default>
      An unknown error has occurred.
    </#switch>
  </p>
</#if>
<form method="post" action="/enter/verify-number/">
  <input maxlength="6" type="text" name="code" placeholder="Please enter the code here." autocomplete="off"/>
  <p class="continue"><button type="submit">Next</button></p>
</form>
<p class="secondary small"><#if attempts gt 0>Click <a href="/enter/reset/?method=${method}">here</a> if you want to change your phone number or request for the <#if method == "SMS">SMS to be send<#else>call to be made</#if> again. </#if>You have ${attempts} attempts <#if method == "SMS">at sending the SMS </#if>left.</p>
</@enter.enter>
