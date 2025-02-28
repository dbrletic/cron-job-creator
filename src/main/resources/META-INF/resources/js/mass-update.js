document.getElementById('regressionForm').addEventListener('submit', function(event) {
    event.preventDefault();

    /*
    const scheduleData = document.getElementById('scheduleData').value;
    const lines = scheduleData.trim().split('\n').filter(line => line.trim() !== ''); */
    const textarea = document.getElementById('scheduleData').value;
     // Remove lines that are only carriage returns, newlines, or empty
    const lines = textarea
     .split('\n') // Split into lines
     .filter(line => line.trim() !== ''); // Keep non-empty lines
    const jobs = [];

    for (let i = 0; i < lines.length; i += 2) {
        if (lines[i]) {
            const job = {
                jobName: lines[i],
                schedule: lines[i + 1]
            };
            jobs.push(job);
        }
    }


    fetch('/ffe-cronjob/mass-update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(jobs)
    })
    .then(response => {
        if (!response.ok) {
            $("<p>Error: " + response +"</p>").appendTo('#errorMessage');
        }
        return response.json();
    })
    .then(data => {
       console.log(data.failedJobs);
       const failedJobs = data.failedJobs;
       failedJobs.forEach(job => {
        $("<p>" + job +"</p>").appendTo('#errorMessage');
       });
        const zipFileContent = atob(data.zipFile);
        const binaryData = new Uint8Array(zipFileContent.length);
        for (let i = 0; i < zipFileContent.length; i++) {
            binaryData[i] = zipFileContent.charCodeAt(i);
        }

        // Create a Blob and trigger the download
        const blob = new Blob([binaryData], { type: 'application/zip' });
        const url = URL.createObjectURL(blob);

        // Create a link element to trigger download
        const link = document.createElement('a');
        link.href = url;
        link.download = 'file.zip';
        document.body.appendChild(link);
        link.click();
    })
    .catch(error => {
        console.error('There was an error:', error);
    });    
    /*
    // Send the jobs data to the Quarkus API
    fetch('/ffe-cronjob/mass-update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(jobs)
    })
    .then(response => {
        if (!response.ok) {
            $("<p>Error: " + response +"</p>").appendTo('#errorMessage');
        if (response.ok) {
            const data = response.message;
            console.log(data);
        }
    }
    return response.blob(); // Convert the response to a Blob
     })
    .then(blob => {
        // Create a link element to download the ZIP file
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
         a.href = url;
        a.download = 'file.zip'; // Specify the filename for download
        document.body.appendChild(a);
        a.click(); // Programmatically click the link to trigger the download
        a.remove(); // Clean up the link element
        URL.revokeObjectURL(url); // Release the object URL
        
    })
    .catch(error => {
        console.error('There was a problem with the fetch operation:', error);
     }); */
 });