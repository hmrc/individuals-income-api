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
        <td><p>fromDate=2016-01-01</p><p>toDate=2017-03-01</p></td>
        <td><p>200 (OK)</p><p>Payload as response example above</p></td>
    </tr>
    <tr>
        <td>Missing fromDate</td>
        <td>fromDate query parameter missing</td>
        <td><p>400 (Bad Request)</p>
        <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;fromDate is required&quot; }</p>
        </td>
    </tr>
    <tr>
         <td><p>toDate earlier than fromDate</p></td>
         <td><p>Any valid dates where toDate is earlier than fromDate</p>
         <p>e.g. fromDate=2017-01-01 toDate=2016-01-01</p></td>
         <td><p>400 (Bad Request)</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;Invalid time period requested&quot; }</p></td>
    </tr>
    <tr>
         <td>From date requested is earlier than available data</td>
         <td>
           <p>fromDate earlier than 31st March 2013</p>
           <p>e.g. fromDate=2013-02-28</p>
         </td>
         <td>
           <p>400 (Bad Request)</p>
           <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;fromDate earlier than 31st March 2013&quot; }</p>
         </td>
    </tr>
    <tr>
         <td><p>Invalid date format</p></td>
         <td><p>Any date that is not ISO 8601 Extended format</p>
         <p>e.g. 20170101</p></td>
         <td><p>400 (Bad Request)</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;fromDate: invalid date format&quot; }</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;toDate: invalid date format&quot; }</p></td>
    </tr>
    <tr>
        <td><p>Incorrect matchId</p></td>
        <td><p>The matchId is not valid</p></td>
        <td><p>404 (Not Found)</p>
        <p>{ &quot;code&quot; : &quot;NOT_FOUND&quot;,<br/>&quot;message&quot; : &quot;The resource cannot be found&quot; }</p></td>
    </tr>
    </tbody>
</table>