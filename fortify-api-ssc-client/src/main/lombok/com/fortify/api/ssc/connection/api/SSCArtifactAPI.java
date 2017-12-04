/*******************************************************************************
 * (c) Copyright 2017 EntIT Software LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.api.ssc.connection.api;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import com.fortify.api.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.api.ssc.connection.api.SSCFileUpDownloadAPI.FileTokenType;
import com.fortify.api.ssc.connection.api.query.SSCApplicationVersionArtifactsQuery;
import com.fortify.api.ssc.connection.api.query.SSCApplicationVersionArtifactsQuery.SSCApplicationVersionArtifactsQueryBuilder;
import com.fortify.api.ssc.connection.api.query.SSCArtifactByIdQuery;
import com.fortify.api.ssc.connection.api.query.SSCArtifactByIdQuery.SSCArtifactByIdQueryBuilder;
import com.fortify.api.util.rest.json.JSONMap;
import com.fortify.api.util.spring.SpringExpressionUtil;

public class SSCArtifactAPI extends AbstractSSCAPI {
	public SSCArtifactAPI(SSCAuthenticatingRestConnection conn) {
		super(conn);
	}
	
	public SSCApplicationVersionArtifactsQueryBuilder queryArtifacts(String applicationVersionId) {
		return SSCApplicationVersionArtifactsQuery.builder().conn(conn()).applicationVersionId(applicationVersionId);
	}
	
	public SSCArtifactByIdQueryBuilder queryArtifacts() {
		return SSCArtifactByIdQuery.builder().conn(conn());
	}
	
	public final JSONMap getArtifactById(String artifactId, boolean useCache, String... fields) {
		return queryArtifacts().id(artifactId).useCache(useCache).paramFields(fields==null?null:Arrays.asList(fields)).build().getUnique();
	}
	
	public final long downloadApplicationFile(String applicationVersionId, Path target, boolean includeSource) {
		WebTarget webTarget = conn().getBaseResource()
				.path("/download/currentStateFprDownload.html")
				.queryParam("id", ""+applicationVersionId)
				.queryParam("includeSource", includeSource);
		return conn().api().fileUpDownload().downloadFile(webTarget, FileTokenType.DOWNLOAD, target);
	}
	
	public final long downloadArtifact(String artifactId, Path target) {
		WebTarget webTarget = conn().getBaseResource()
				.path("/download/artifactDownload.html")
				.queryParam("id", ""+artifactId);
		return conn().api().fileUpDownload().downloadFile(webTarget, FileTokenType.DOWNLOAD, target);
	}
	
	public final JSONMap uploadArtifact(String applicationVersionId, File fprFile) {
		WebTarget webTarget = conn().getBaseResource()
				.path("/upload/resultFileUpload.html")
				.queryParam("entityId", ""+applicationVersionId);
		return conn().api().fileUpDownload().uploadFile(webTarget, FileTokenType.UPLOAD, fprFile);
	}
	
	public final JSONMap approveArtifact(String artifactId, String comment) {
		JSONMap data = new JSONMap();
		data.putPath("type", "approve");
		data.putPath("values.comment", comment);
		return conn().executeRequest(HttpMethod.POST,
				conn().getBaseResource().path("/api/v1/artifacts/").path(artifactId).path("/action"), 
				Entity.entity(data, "application/json"), JSONMap.class);
	}
	
	public final JSONMap getJobForUpload(JSONMap uploadResult, int secondsToWaitForCompletion) {
		String jobId = uploadResult.get("id", String.class);
		return conn().api().job().waitForJobCompletion(jobId, secondsToWaitForCompletion);
	}
	
	public final String getArtifactIdForUploadJob(JSONMap job) {
		return SpringExpressionUtil.evaluateExpression(job, "jobData.PARAM_ARTIFACT_ID", String.class);
	}
	
	public final String uploadArtifactAndWaitProcessingCompletion(String applicationVersionId, File fprFile, int timeOutSeconds) {
		JSONMap uploadResult = uploadArtifact(applicationVersionId, fprFile);
		JSONMap job = getJobForUpload(uploadResult, timeOutSeconds);
		return getArtifactIdForUploadJob(job);
	}

	public static void main(String[] args) throws InterruptedException {
		SSCAuthenticatingRestConnection conn = new SSCAuthenticatingRestConnection("http://localhost:1710/ssc", "ssc",  "Admin123!", null);
		String artifactId = conn.api().artifact().uploadArtifactAndWaitProcessingCompletion("6", new File("c:/work/Programs/HP/SCA/17.20/samples/basic/sampleOutput/WebGoat5.0.fpr"), 60);
		System.out.println(conn.api().artifact().getArtifactById(artifactId, true));
		System.out.println(conn.api().artifact().getArtifactById(artifactId, true).get("uploadDate", Date.class).getClass().getName());
	}
}
