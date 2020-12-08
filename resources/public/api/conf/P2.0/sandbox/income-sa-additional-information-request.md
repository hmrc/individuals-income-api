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
        <td><p>Additional information data found</p></td>
        <td><p>matchId=&lt;obtained from Individuals Matching API. example: 57072660-1df9-4aeb-b4ea-cd2d7f96e430&gt;</p><p>startYear=2018-19</p><p>endTaxYear=2019-20</p></td>
        <td><p>200 (OK)</p><p>Payload as response example above</p></td>
    </tr>
    <tr>
        <td>Missing matchId</td>
        <td>matchId query parameter missing</td>
        <td><p>400 (Bad Request)</p>
        <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;matchId is required&quot; }</p>
        </td>
    </tr>
    <tr>
        <td>Missing startYear</td>
        <td>startYear query parameter missing</td>
        <td><p>400 (Bad Request)</p>
        <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;startYear is required&quot; }</p>
        </td>
    </tr>
    <tr>
         <td><p>endYear earlier than startYear</p></td>
         <td><p>Any valid dates where endYear is earlier than startYear. For example, startYear=2016-17 endYear=2015-16</p></td>
         <td><p>400 (Bad Request)</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;Invalid time period requested&quot; }</p></td>
    </tr>
    <tr>
         <td><p>endYear later than the current tax year</p></td>
         <td><p>For example, startYear=2016-17 endYear=2098-99</p></td>
         <td><p>400 (Bad Request)</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;endYear is later than the current tax year&quot; }</p></td>
    </tr>
    <tr>
         <td>startYear earlier than the current tax year minus 6</td>
         <td><p>For example, startYear=2008-09</p>
         </td>
         <td>
           <p>400 (Bad Request)</p>
           <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;startYear earlier than maximum allowed&quot; }</p>
         </td>
    </tr>
    <tr>
         <td><p>Invalid tax year format</p></td>
         <td><p>Any tax year that is not in the format YYYY-YY</p>
         <p>For example, 2017-2018</p></td>
         <td><p>400 (Bad Request)</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;startYear: invalid tax year format&quot; }</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;endYear: invalid tax year format&quot; }</p></td>
    </tr>
    <tr>
        <td><p>Incorrect matchId</p></td>
        <td><p>The matchId is not valid</p></td>
        <td><p>404 (Not Found)</p>
        <p>{ &quot;code&quot; : &quot;NOT_FOUND&quot;,<br/>&quot;message&quot; : &quot;The resource cannot be found&quot; }</p></td>
    </tr>
    </tbody>
</table>