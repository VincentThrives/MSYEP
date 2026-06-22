import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * KP-MSYEP / Yuktha Kaushalya Kar job-portal Terms & Conditions.
 * Reused at every "accept the T&C" step (franchise sign-up, employer/candidate sign-up).
 */
@Component({
  selector: 'app-terms',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="terms">
      <h2>Terms &amp; Conditions</h2>

      <h3>Preamble</h3>
      <p>The User of the Job Portal / website (hereinafter referred to as “User”) agrees to the
        following terms and conditions, including any future amendments (hereinafter collectively
        referred to as the “Terms &amp; Conditions”) before using the Job Portal/website
        <a href="https://jobs.yukthakaushalyakar.in/" target="_blank" rel="noopener">https://jobs.yukthakaushalyakar.in/</a>
        (hereinafter referred to as “Job Portal”). By using the Job Portal, the Users are giving their
        consent to be bound by the stipulations as contained under the caption “Terms &amp; Conditions” below.</p>

      <h3>Purpose of the Job portal</h3>
      <p>The purpose of this job portal is to provide a common platform for all kinds of job seekers
        with relevant qualifications, experience (Any Diploma, Graduates, Post Graduates etc) and for the
        Companies who are searching to recruit a desired candidates with both Technical and Non-Technical Skillsets.</p>

      <h3>General Terms &amp; Conditions</h3>
      <ul>
        <li>These Terms and Conditions and the Privacy Policy Statement, which are hereby incorporated as if
          set forth fully herein, represent the complete agreement between you and the Company for the use of
          and access to the Services and/or other contents of the Job Portal.</li>
        <li>The job portal reserves the right to decline service/registration to any person at any point of time
          without giving any prior notice. It is clarified that registering with the portal does not guarantee employment.</li>
        <li>The User agrees that the services of the job portal, once subscribed to by the User are not refundable
          and all amount/s paid shall stand appropriated.</li>
        <li>Nothing in this Agreement shall be deemed to confer any third-party rights or benefits.</li>
        <li>The job portal authorizes the User to view and access the content available on or from the Job Portal
          solely for their personal use. The contents of the Job Portal (text, graphics, images, logos, button icons,
          software, and other contents as well as the compilation thereof) are property of the job portal. Unauthorized
          use will be a violation of copyright and other intellectual property rights. The User may not sell or modify
          the said content or reproduce, display, publicly perform, distribute, or otherwise use it for any public or
          commercial purpose.</li>
        <li>The User shall be solely responsible for maintaining the confidentiality of their account and passwords,
          and for all activities under his/her username whether authorized or not, and agrees to immediately notify the
          Company of any unauthorized use of his Account and Password.</li>
        <li>The job portal reserves the right to modify, suspend, cancel, discontinue, or terminate the services or
          reject any or all entries/blogs at its absolute discretion without prior notice, without assigning any reason,
          and without any liability.</li>
        <li>The User shall be responsible for the use of the Job Portal and for any entries/posts uploaded, and shall use
          the Job Portal in accordance with all applicable laws (including IT law and laws relating to unfair competition,
          anti-discrimination, false advertising, and defamation) and shall not violate any third-party rights. The User
          shall not transmit, distribute, store, or destroy material that is defamatory, obscene, threatening, abusive,
          or hateful. Any violation shall result in immediate termination of access.</li>
      </ul>

      <h3>Prohibited security violations</h3>
      <p>Users are prohibited from violating or attempting to violate the security of the Job Portal, including:
        (a) accessing data not intended for the User or logging into a server/account they are not authorized to access;
        (b) attempting to probe, scan or test the vulnerability of a system/network or breach security/authentication
        without authorization; (c) attempting to interfere with service to any User, host, or network (virus, overloading,
        “flooding”, “spamming”, “mailbombing”, “crashing”); or (d) forging any TCP/IP packet header or header information
        in any e-mail or newsgroup posting. Violations may result in civil or criminal liability.</p>

      <h3>Prohibited User Content</h3>
      <p>The list below is for illustration only and is not comprehensive. User Content that:</p>
      <ul>
        <li>is implicitly or explicitly offensive (racism, bigotry, hatred, or physical harm against any group/individual);</li>
        <li>harasses, incites or advocates harassment of another group/individual;</li>
        <li>involves “junk mail”, “chain letters,” unsolicited mass mailing or “spamming”;</li>
        <li>promotes false or misleading information or illegal/abusive/threatening/obscene/defamatory conduct;</li>
        <li>promotes illegal or unauthorized use of any copyrighted work (pirated software/music/media or links thereto);</li>
        <li>contains restricted or password-only pages, or hidden pages/images;</li>
        <li>displays or links to obscene, indecent, pornographic material of any kind;</li>
        <li>provides instructions for illegal activities, or solicits passwords/personal identifying information from other Users.</li>
      </ul>

      <h3>Content, disclosure &amp; data</h3>
      <p>The contents, views, and ideas posted are that of the respective Users; the job portal does not endorse them and
        makes no representations about any jobs, resumes or User Content. The job portal is not involved in the actual
        transaction between employers and candidates; any reliance on posted content is entirely at the User’s risk.</p>
      <p>The job portal reserves its right to access, read, preserve, and/or disclose the personal information of the User
        (including postings) without prior notice where required to comply with any court order or legal/government authority,
        by applicable law, by its advisers/employees, to investigate violations, to respond to service-support requests, or
        to protect the rights, property, or safety of the job portal and the public. The job portal may store and process
        personal information in India or any other country where it or its agents maintain facilities, and the User consents
        to such transfer outside India without prior notice.</p>

      <h3>Warranties &amp; liability</h3>
      <p>The job portal does not warrant error-free or virus-free operation and, to the fullest extent permitted by law,
        disclaims all warranties (merchantability, fitness for a particular purpose, non-infringement) and makes no
        warranties about the accuracy, reliability, completeness, or timeliness of content. <b>The job portal’s maximum
        liability arising out of or in connection with the website or the User’s use of its content, regardless of the cause
        of action, will not exceed Rs. 5,000/- under any circumstances whatsoever.</b></p>
      <p>The User agrees to indemnify the job portal and its owner/directors/partners/employees and agents against any
        third-party claim arising out of the User’s use of the services or content submitted in violation of these terms.
        The job portal is not liable if a company changes/holds a requirement after interviews, for any agreement/financial
        understanding between the User and the proposed employer, or if an employer changes the date of joining or charges
        for training and reneges on a job commitment.</p>

      <h3>Governing law &amp; dispute resolution</h3>
      <p>These terms shall be interpreted under Indian law. Civil disputes shall be referred to a mutually appointed panel
        advocate of an NGO dealing with consumer protection for mediation/conciliation/arbitration; arbitration shall be
        governed by the Arbitration and Conciliation Act, 1996, by a single arbitrator (a retired Judge or an advocate with
        at least ten years’ civil practice). For any private criminal proceedings, the Courts in Mumbai shall have exclusive
        jurisdiction.</p>

      <h3>Stipulations for employers</h3>
      <ul>
        <li>One-time Subscription Charges of <b>INR 9977/-</b> (Indian Rupees Nine Thousand Nine Hundred and Seventy Seven
          only) will be collected from Employers during signup/registration to opt for job-listing service. Prospective
          employers can register, post jobs, search candidates, and announce walk-in interviews.</li>
        <li>Each prospective employer can upload <b>max. 12 job postings</b> (with email-alert facility). Additional slots
          are provided at the applicable additional price on prior intimation.</li>
        <li>Employers shall post only genuine technical-job requirements, shall not post fake/misleading advertisements,
          shall not charge candidates unless their guidelines clearly state so, and shall be fully transparent with candidates.</li>
        <li>Amounts remitted for advertisement and additional services are not refundable under any circumstances. The job
          portal may use registered employers’ names and logos for marketing and branding.</li>
        <li><b>General Category</b> candidates are charged <b>INR 497/-</b> and <b>Other Category</b> candidates <b>INR 270/-</b>
          as one-time subscription charges for portal and profile maintenance.</li>
      </ul>

      <h3>Note: Empowerment of Local Employers</h3>
      <ul>
        <li>MSYEP under “Skill Path” (ಕೌಶಲ್ಯ ಪಥ) collaborates for the empowerment of local employers with their self-interest.</li>
        <li>Local organizations associate with MSYEP training center branches through the online registration link
          <a href="https://jobs.yukthakaushalyakar.in/user-registration" target="_blank" rel="noopener">https://jobs.yukthakaushalyakar.in/user-registration</a>,
          obtaining official approval through registration and discussion with the Admin wing.</li>
        <li>MSYEP branches and organised trainings shall run as per the MSYEP Training Guideline; otherwise the collaboration
          with such local organization will be cancelled.</li>
        <li>If a local organization wishes to discontinue collaboration, it must inform MSYEP by a proper reasoned email to
          <b>ceo&#64;msyep.in</b> 60 days in advance.</li>
        <li>Any hard-copy documents related to MSYEP training left at local organizations must be submitted within 7 days
          from training completion / the “not-interested” email intimation, failing which legal action will be taken.</li>
      </ul>

      <h3>Interpretation</h3>
      <p>Wherever the male gender is referred to (“his”, “him”, “he”), it includes the female gender. Wherever the context
        requires, “job portal” means and includes the job portal’s owner/partner/director.</p>

      <h3>Service Cancellation and Refund Policy</h3>
      <ul>
        <li>Service charges are not refundable under any circumstances once the job-portal service is activated to the employer.</li>
        <li>For duplicate/multiple payments accidentally transferred for a single subscription, the subscriber must notify the
          job portal within 3 days of the payment date with proper payment proof; the additional payment will be reversed within
          seven days after a cross-check with the accounts section.</li>
        <li>If the User fails to notify within 3 days from the transaction date, the job portal disclaims its liability to refund.</li>
      </ul>
      <p class="muted">Terms and Conditions may be subject to periodic changes. Please review the latest version at
        <a href="https://jobs.yukthakaushalyakar.in/" target="_blank" rel="noopener">https://jobs.yukthakaushalyakar.in/</a>.</p>
    </div>
  `,
  styles: [`
    .terms { font-size: 13.5px; line-height: 1.6; color: #333; }
    .terms h2 { color: #0E5132; margin: 0 0 12px; }
    .terms h3 { color: #0E5132; margin: 18px 0 6px; font-size: 15px;
      border-left: 4px solid #C9A227; padding-left: 8px; }
    .terms ul { margin: 6px 0; padding-left: 20px; }
    .terms li { margin-bottom: 6px; }
    .terms a { color: #1E7A46; }
    .terms .muted { color: #888; font-size: 12px; margin-top: 14px; }
  `],
})
export class TermsComponent {}
