<#ftl output_format="HTML">
<#import "enter.ftl" as enter/>
<@enter.enter title="Verify your Jabber ID">
<p>We have sent you a Jabber message to ${jid} containing a verification code.</p>
<#if error??>
  <p class="error">
    <#switch error>
      <#case "invalid">
      The code you have entered is invalid.
      <#break>
      <#case "failed">
      Unable to send verification message.
      <#break>
      <#default>
      An unknown error has occurred.
    </#switch>
  </p>
</#if>
<form method="post" action="/enter/verify-jabber/">
  <input type="text" name="code" placeholder="Please enter the code here." autocomplete="off"/>
  <p class="continue"><button type="submit">Next</button></p>
</form>
</@enter.enter>
