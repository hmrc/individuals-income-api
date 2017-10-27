<table>
    <col width="25%">
    <col width="35%">
    <col width="40%">
    <thead>
    <tr>
        <th>Scenario</th>
        <th>Parameters</th>
        <th>Response</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td><p>Happy path</p></td>
        <td><p>fromTaxYear=2013-14</p><p>toTaxYear=2015-16</p></td>
        <td><p>200 (OK)</p><p>Payload as response example above</p></td>
    </tr>
    <tr>
        <td>Missing fromTaxYear</td>
        <td>fromTaxYear query parameter missing</td>
        <td><p>400 (Bad Request)</p>
        <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;fromTaxYear is required&quot; }</p>
        </td>
    </tr>
    <tr>
         <td><p>toTaxYear earlier than fromTaxYear</p></td>
         <td><p>Any valid tax year where toTaxYear is earlier than fromTaxYear</p>
         <p>e.g. fromTaxYear=2016-17 toTaxYear=2015-16</p></td>
         <td><p>400 (Bad Request)</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;Invalid time period requested&quot; }</p></td>
    </tr>
    <tr>
         <td>From tax year requested is earlier than available data</td>
         <td>
           <p>fromTaxYear earlier than current tax year minus 7</p>
           <p>e.g. fromTaxYear=2008-09</p>
         </td>
         <td>
           <p>400 (Bad Request)</p>
           <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;fromTaxYear earlier than maximum allowed&quot; }</p>
         </td>
    </tr>
    <tr>
         <td><p>Invalid tax year format</p></td>
         <td><p>Any tax year that is not in the format YYYY-YY</p>
         <p>e.g. 2017-2018</p></td>
         <td><p>400 (Bad Request)</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;fromTaxYear: invalid tax year format&quot; }</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;toTaxYear: invalid tax year format&quot; }</p></td>
    </tr>
    <tr>
        <td><p>Incorrect matchId</p></td>
        <td><p>The matchId is not valid</p></td>
        <td><p>404 (Not Found)</p>
        <p>{ &quot;code&quot; : &quot;NOT_FOUND&quot;,<br/>&quot;message&quot; : &quot;The resource cannot be found&quot; }</p></td>
    </tr>
    </tbody>
</table>